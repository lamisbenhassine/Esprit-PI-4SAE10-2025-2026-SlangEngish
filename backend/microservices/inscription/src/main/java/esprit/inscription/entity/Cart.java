package esprit.inscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> cartItems = new ArrayList<>();

    public void addItem(CartItem item) {
        cartItems.add(item);
        item.setCart(this);
        updateTotalAmount();
    }

    public void removeItem(CartItem item) {
        cartItems.remove(item);
        item.setCart(null);
        updateTotalAmount();
    }

    public void updateTotalAmount() {
        this.totalAmount = cartItems.stream()
                .mapToDouble(CartItem::getUnitPrice)
                .sum();
    }
}
