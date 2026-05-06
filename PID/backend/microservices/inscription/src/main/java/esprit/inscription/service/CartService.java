package esprit.inscription.service;

import esprit.inscription.entity.*;
import esprit.inscription.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPricingService subscriptionPricingService;
    private final UserRepository userRepository;
    private final EmailCampaignService emailCampaignService;

    /** Returns the cart for the user; when multiple exist (legacy data), the latest by id (most likely to have items). */
    public Optional<Cart> getCartByUserId(Long userId) {
        return cartRepository.findFirstByUserIdOrderByIdDesc(userId);
    }

    /**
     * Get existing cart or create one. Handles race where another request already created a cart.
     */
    public Cart getOrCreateCart(Long userId) {
        log.info("Getting or creating cart for user: {}", userId);
        
        try {
            // First try to find existing cart
            Optional<Cart> existing = cartRepository.findFirstByUserIdOrderByIdDesc(userId);
            if (existing.isPresent()) {
                log.info("Found existing cart for user {}: {}", userId, existing.get().getId());
                return existing.get();
            }
            
            // Create new cart
            log.info("Creating new cart for user: {}", userId);
            Cart newCart = Cart.builder()
                    .userId(userId)
                    .totalAmount(BigDecimal.ZERO)
                    .cartItems(new ArrayList<>())
                    .build();
            
            Cart savedCart = cartRepository.saveAndFlush(newCart);
            log.info("Successfully created cart for user {}: {}", userId, savedCart.getId());
            return savedCart;
            
        } catch (Exception e) {
            log.error("Error creating cart for user: {}", userId, e);
            
            // Try to fetch existing cart in case of race condition
            return cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Could not get or create cart for user: " + userId + ". Error: " + e.getMessage()));
        }
    }

    @Transactional
    public Cart addOfferToCart(Long userId, Long subscriptionPlanId) {
        log.info("Adding offer {} to cart for user {}", subscriptionPlanId, userId);
        
        try {
            Cart cart = getOrCreateCart(userId);
            log.info("Cart retrieved/created: {}", cart.getId());
            
            // Get subscription plan details
            SubscriptionPlan plan = subscriptionPlanRepository.findById(subscriptionPlanId)
                    .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + subscriptionPlanId));
            
            log.info("Plan found: {} - Price: {}", plan.getName(), plan.getPrice());

            // Prevent duplicate plans in cart
            boolean alreadyExists = cart.getCartItems().stream()
                    .anyMatch(item -> item.getSubscriptionPlanId().equals(subscriptionPlanId));

            if (alreadyExists) {
                log.warn("Offer {} already in cart for user {}", subscriptionPlanId, userId);
                return cart;
            }

            // Create cart item with full details
            CartItem item = CartItem.builder()
                    .subscriptionPlanId(subscriptionPlanId)
                    .unitPrice(plan.getPrice())
                    .planName(plan.getName())
                    .planType(plan.getPlanType())
                    .durationDays(plan.getDurationDays())
                    .quantity(1)
                    .addedAt(LocalDateTime.now())
                    .build();

            log.info("CartItem created: {}", item.getPlanName());
            
            cart.addItem(item);
            cart = cartRepository.save(cart);
            
            log.info("Successfully added offer {} to cart for user {}. Cart total: {}", 
                    subscriptionPlanId, userId, cart.getTotalAmount());
            return cart;
            
        } catch (Exception e) {
            log.error("Error adding offer to cart for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to add offer to cart: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Cart addItemToCart(Long userId, Long subscriptionPlanId, BigDecimal unitPrice, String planName) {
        log.info("Adding item to cart for user {}: planId={}, price={}", userId, subscriptionPlanId, unitPrice);
        
        Cart cart = getOrCreateCart(userId);

        // Prevent duplicate plans in cart
        boolean alreadyExists = cart.getCartItems().stream()
                .anyMatch(item -> item.getSubscriptionPlanId().equals(subscriptionPlanId));

        if (alreadyExists) {
            log.warn("Item already exists in cart for user {}: planId={}", userId, subscriptionPlanId);
            return cart;
        }

        CartItem item = new CartItem();
        item.setSubscriptionPlanId(subscriptionPlanId);
        item.setUnitPrice(unitPrice);
        item.setPlanName(planName);
        item.setQuantity(1);
        item.setAddedAt(LocalDateTime.now());

        cart.addItem(item);
        cart = cartRepository.save(cart);
        
        log.info("Successfully added item to cart for user {}", userId);
        return cart;
    }

    @Transactional
    public Cart addItemToCart(Long userId, Long subscriptionPlanId) {
        log.info("Adding item to cart for user {}: planId={}", userId, subscriptionPlanId);
        
        // Get subscription plan details
        SubscriptionPlan plan = subscriptionPlanRepository.findById(subscriptionPlanId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + subscriptionPlanId));

        return addItemToCart(userId, subscriptionPlanId, plan.getPrice(), plan.getName());
    }

    /**
     * Métier avancé : ajout au panier avec réduction frères/sœurs.
     * Utilise le plan déjà chargé pour calculer la réduction (logique unique, pas de double appel).
     */
    @Transactional
    public Cart addWithSiblingDiscount(Long userId, Long subscriptionPlanId, int siblingCount) {
        if (siblingCount < 2) {
            throw new IllegalArgumentException("Réduction fratrie : au moins 2 frères/sœurs requis");
        }
        SubscriptionPlan plan = subscriptionPlanRepository.findById(subscriptionPlanId)
                .orElseThrow(() -> new RuntimeException("Plan non trouvé (id " + subscriptionPlanId + "). Choisissez un plan proposé sur la page offres."));
        // Calcul à partir du plan déjà chargé (évite double fetch et erreur si prix null en base)
        var discount = subscriptionPricingService.computeSiblingDiscount(plan, siblingCount);
        if (discount == null) {
            throw new RuntimeException("Le plan \"" + (plan.getName() != null ? plan.getName() : plan.getPlanType()) + "\" n'a pas de prix renseigné. Définissez un prix en back-office ou choisissez un autre plan.");
        }
        Cart cart = getOrCreateCart(userId);

        // Remplacer toute ligne existante pour ce plan par la ligne avec réduction fratrie
        cart.getCartItems().stream()
                .filter(item -> subscriptionPlanId.equals(item.getSubscriptionPlanId()))
                .toList()
                .forEach(item -> {
                    cart.removeItem(item);
                    cartItemRepository.delete(item);
                });

        CartItem item = CartItem.builder()
                .subscriptionPlanId(subscriptionPlanId)
                .quantity(siblingCount)
                .unitPrice(discount.getPricePerPersonAfterDiscount())
                .planName((plan.getName() != null ? plan.getName() : plan.getPlanType()) + " (réduction fratrie " + siblingCount + ")")
                .planType(plan.getPlanType() != null ? plan.getPlanType() : "BASIC")
                .durationDays(plan.getDurationDays() != null ? plan.getDurationDays() : 30)
                .addedAt(LocalDateTime.now())
                .build();
        cart.addItem(item);
        log.info("Added to cart with sibling discount: user={}, planId={}, siblings={}, unitPrice={}", userId, subscriptionPlanId, siblingCount, discount.getPricePerPersonAfterDiscount());
        return cartRepository.save(cart);
    }

    /**
     * Métier avancé : ajout au panier en tarif famille (N personnes, prix par personne = total / N).
     */
    @Transactional
    public Cart addWithFamilyRate(Long userId, Long subscriptionPlanId, int numberOfPeople) {
        if (numberOfPeople < 2) {
            throw new IllegalArgumentException("Tarif famille : au moins 2 personnes requis");
        }
        SubscriptionPlan plan = subscriptionPlanRepository.findById(subscriptionPlanId)
                .orElseThrow(() -> new RuntimeException("Plan non trouvé (id " + subscriptionPlanId + "). Choisissez un plan proposé sur la page offres."));
        BigDecimal totalPrice = plan.getPrice() != null && plan.getPrice().compareTo(BigDecimal.ZERO) > 0
                ? plan.getPrice()
                : new BigDecimal("49.00");
        BigDecimal pricePerPerson = totalPrice.divide(BigDecimal.valueOf(numberOfPeople), 2, java.math.RoundingMode.HALF_UP);
        Cart cart = getOrCreateCart(userId);
        cart.getCartItems().stream()
                .filter(item -> subscriptionPlanId.equals(item.getSubscriptionPlanId()))
                .toList()
                .forEach(item -> {
                    cart.removeItem(item);
                    cartItemRepository.delete(item);
                });
        CartItem item = CartItem.builder()
                .subscriptionPlanId(subscriptionPlanId)
                .quantity(numberOfPeople)
                .unitPrice(pricePerPerson)
                .planName((plan.getName() != null ? plan.getName() : plan.getPlanType()) + " (tarif famille " + numberOfPeople + ")")
                .planType(plan.getPlanType() != null ? plan.getPlanType() : "BASIC")
                .durationDays(plan.getDurationDays() != null ? plan.getDurationDays() : 30)
                .addedAt(LocalDateTime.now())
                .build();
        cart.addItem(item);
        log.info("Added to cart with family rate: user={}, planId={}, people={}, unitPrice={}", userId, subscriptionPlanId, numberOfPeople, pricePerPerson);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItemFromCart(Long userId, Long itemId) {
        log.info("Removing item {} from cart for user {}", itemId, userId);
        
        Cart cart = cartRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem itemToRemove = cart.getCartItems().stream()
                .filter(item -> item.getId() != null && item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart: " + itemId));

        cart.removeItem(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        cart = cartRepository.saveAndFlush(cart);
        
        log.info("Successfully removed item from cart for user {}", userId);
        return cart;
    }

    @Transactional
    public Cart removeItemByPlanId(Long userId, Long subscriptionPlanId) {
        log.info("Removing item by planId {} from cart for user {}", subscriptionPlanId, userId);
        
        Cart cart = cartRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem itemToRemove = cart.getCartItems().stream()
                .filter(item -> subscriptionPlanId.equals(item.getSubscriptionPlanId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart for plan: " + subscriptionPlanId));

        cart.removeItem(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        cart = cartRepository.saveAndFlush(cart);
        
        log.info("Successfully removed item by planId from cart for user {}", userId);
        return cart;
    }

    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);
        cartRepository.findFirstByUserIdOrderByIdDesc(userId).ifPresentOrElse(this::clearCartContents, () -> {
            log.warn("clearCart: no cart row for user {} (nothing to clear)", userId);
        });
    }

    /**
     * Vide le panier déjà chargé dans la transaction courante.
     * Préféré après création de commande : évite un second SELECT par userId qui peut échouer (plusieurs paniers legacy, cache, etc.).
     */
    @Transactional
    public void clearCartContents(Cart cart) {
        if (cart == null) {
            return;
        }
        log.info("Clearing cart id {} for user {}", cart.getId(), cart.getUserId());
        cart.getCartItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);
        log.info("Successfully cleared cart id {}", cart.getId());
    }

    @Transactional
    public Order checkoutCart(Long userId, String paymentMethod, String promoCode) {
        log.info("Checking out cart for user {} with payment method: {}", userId, paymentMethod);
        
        Cart cart = cartRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty for user: " + userId);
        }

        // Get user details
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(generateOrderNumber());
        order.setTotalAmount(cart.getTotalAmount());
        order.setPaymentMethod(paymentMethod);
        order.setPromoCode(promoCode);

        // Add items to order
        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setSubscriptionPlanId(cartItem.getSubscriptionPlanId());
            orderItem.setItemName(cartItem.getPlanName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            order.addItem(orderItem);
        }

        // Save order (this would typically be in an OrderService)
        Order savedOrder = order; // orderRepository.save(order);

        // Clear cart after successful checkout
        clearCart(userId);

        // Send confirmation email
        sendPurchaseConfirmationEmail(user, savedOrder);

        log.info("Successfully checked out cart for user {}. Order: {}", userId, savedOrder.getOrderNumber());
        return savedOrder;
    }

    private void sendPurchaseConfirmationEmail(User user, Order order) {
        log.info("Sending purchase confirmation email to user {} for order {}", user.getId(), order.getOrderNumber());
        
        try {
            // Create purchase confirmation campaign
            EmailCampaign campaign = EmailCampaign.builder()
                    .name("Purchase Confirmation - " + order.getOrderNumber())
                    .category(EmailCampaign.CampaignCategory.PROMOTIONAL)
                    .subject("Purchase Confirmation - Order #" + order.getOrderNumber())
                    .fromEmail("orders@english-academy.com")
                    .fromName("English Academy")
                    .targetLevel(user.getEnglishLevel())
                    .build();

            EmailCampaign createdCampaign = emailCampaignService.createCampaign(campaign);
            
            // Launch campaign for this specific user
            emailCampaignService.launchCampaign(createdCampaign.getId());
            
            log.info("Purchase confirmation email sent successfully to user {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send purchase confirmation email to user {}", user.getId(), e);
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    public BigDecimal getCartTotal(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cart.getTotalAmount();
    }

    public int getCartItemCount(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cart.getCartItems().size();
    }
}
