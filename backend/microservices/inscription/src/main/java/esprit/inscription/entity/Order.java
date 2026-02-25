package esprit.inscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }
}
