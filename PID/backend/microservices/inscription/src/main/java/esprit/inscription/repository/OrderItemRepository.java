package esprit.inscription.repository;

import esprit.inscription.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Lignes de commande pour commandes ayant au moins un paiement complété (revenus réels).
     */
    @Query(value = """
            SELECT COALESCE(sp.name, oi.item_name, 'Autre') AS plan_label,
                   COUNT(*) AS cnt,
                   COALESCE(SUM(oi.total_price), 0) AS rev
            FROM order_item oi
            INNER JOIN orders o ON oi.order_id = o.id
            INNER JOIN payment p ON p.order_id = o.id AND LOWER(TRIM(p.status)) = 'completed'
            LEFT JOIN subscription_plan sp ON oi.subscription_plan_id = sp.id
            GROUP BY COALESCE(sp.name, oi.item_name, 'Autre')
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> aggregatePaidOrdersByPlanLabel();

    /** Toutes les lignes de commande (sans exiger un paiement complété — repli dashboard). */
    @Query(value = """
            SELECT COALESCE(sp.name, oi.item_name, 'Autre') AS plan_label,
                   COUNT(*) AS cnt,
                   COALESCE(SUM(oi.total_price), 0) AS rev
            FROM order_item oi
            INNER JOIN orders o ON oi.order_id = o.id
            LEFT JOIN subscription_plan sp ON oi.subscription_plan_id = sp.id
            GROUP BY COALESCE(sp.name, oi.item_name, 'Autre')
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> aggregateAllOrdersByPlanLabel();
}
