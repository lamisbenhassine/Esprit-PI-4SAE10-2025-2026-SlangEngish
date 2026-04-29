package esprit.inscription.controller;

import esprit.inscription.entity.Cart;
import esprit.inscription.entity.PromoCode;
import esprit.inscription.repository.SubscriptionPlanRepository;
import esprit.inscription.service.CartService;
import esprit.inscription.service.EmailService;
import esprit.inscription.service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PromoCodeService promoCodeService;

    private CartController cartController;

    @BeforeEach
    void setUp() {
        cartController = new CartController(cartService, subscriptionPlanRepository, emailService, promoCodeService);
    }

    @Test
    void createCart_shouldReturnOkWhenServiceSucceeds() {
        Cart cart = mock(Cart.class);
        when(cartService.getOrCreateCart(1L)).thenReturn(cart);

        ResponseEntity<Cart> response = cartController.createCart(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(cart, response.getBody());
    }

    @Test
    void addOfferToCart_shouldReturnBadRequestWhenRequestIsInvalid() {
        CartController.AddOfferRequest request = new CartController.AddOfferRequest();
        request.setUserId(null);
        request.setSubscriptionPlanId(5L);

        ResponseEntity<Object> response = cartController.addOfferToCart(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request: userId and subscriptionPlanId are required", response.getBody());
    }

    @Test
    void addOfferToCart_shouldUseErrorPrefixOnRuntimeException() {
        CartController.AddOfferRequest request = new CartController.AddOfferRequest();
        request.setUserId(2L);
        request.setSubscriptionPlanId(3L);

        when(cartService.addOfferToCart(2L, 3L)).thenThrow(new RuntimeException("plan not found"));

        ResponseEntity<Object> response = cartController.addOfferToCart(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error: plan not found", response.getBody());
    }

    @Test
    void addOfferToCart_shouldReturnOkWhenServiceSucceeds() {
        CartController.AddOfferRequest request = new CartController.AddOfferRequest();
        request.setUserId(2L);
        request.setSubscriptionPlanId(3L);

        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(10L);
        when(cart.getTotalAmount()).thenReturn(BigDecimal.TEN);
        when(cartService.addOfferToCart(2L, 3L)).thenReturn(cart);

        ResponseEntity<Object> response = cartController.addOfferToCart(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(cart, response.getBody());
    }

    @Test
    void testPromoCode_shouldReturnSuccessMessageWhenPromoIsValid() {
        PromoCodeService.PromoCodeValidationResult validation =
                PromoCodeService.PromoCodeValidationResult.valid("123", new BigDecimal("5.00"));

        when(promoCodeService.create(any(PromoCode.class))).thenReturn(mock(PromoCode.class));
        when(promoCodeService.validate("123", new BigDecimal("50")))
                .thenReturn(Optional.of(validation));

        ResponseEntity<String> response = cartController.testPromoCode("123", new BigDecimal("50"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("CODE PROMO VALIDE"));
        assertTrue(response.getBody().contains("Réduction: 5.00€"));
    }

    @Test
    void testPromoCode_shouldReturnInvalidMessageWhenPromoIsInvalid() {
        PromoCodeService.PromoCodeValidationResult invalid =
                PromoCodeService.PromoCodeValidationResult.invalid("Ce code a expiré.");

        when(promoCodeService.create(any(PromoCode.class))).thenReturn(mock(PromoCode.class));
        when(promoCodeService.validate("123", new BigDecimal("50")))
                .thenReturn(Optional.of(invalid));

        ResponseEntity<String> response = cartController.testPromoCode("123", new BigDecimal("50"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("❌ CODE INVALIDE: Ce code a expiré.", response.getBody());
    }

    @Test
    void testPromoCode_shouldHandleEmptyValidationResult() {
        when(promoCodeService.create(any(PromoCode.class))).thenReturn(mock(PromoCode.class));
        when(promoCodeService.validate("123", new BigDecimal("50"))).thenReturn(Optional.empty());

        ResponseEntity<String> response = cartController.testPromoCode("123", new BigDecimal("50"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("❌ ERREUR: Pas de réponse du service", response.getBody());
    }

    @Test
    void testPromoCode_shouldReturnServerErrorWhenServiceThrows() {
        when(promoCodeService.create(any(PromoCode.class))).thenThrow(new RuntimeException("service down"));

        ResponseEntity<String> response = cartController.testPromoCode("123", new BigDecimal("50"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("❌ ERREUR: service down", response.getBody());
    }
}
