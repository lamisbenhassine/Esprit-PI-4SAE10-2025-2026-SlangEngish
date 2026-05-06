package esprit.inscription.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * En dev, la table {@code orders} peut encore avoir une FK {@code user_id} → {@code users.id}
 * alors que le panier utilise l’id du user principal (users 8011) sans ligne correspondante
 * dans {@code inscription.users} → INSERT commande = 400. On supprime cette FK au démarrage si présente.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class OrdersUserForeignKeyRelaxer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                            SELECT CONSTRAINT_NAME
                            FROM information_schema.KEY_COLUMN_USAGE
                            WHERE TABLE_SCHEMA = DATABASE()
                              AND TABLE_NAME = 'orders'
                              AND COLUMN_NAME = 'user_id'
                              AND REFERENCED_TABLE_NAME = 'users'
                            """);
            for (Map<String, Object> row : rows) {
                Object cn = row.get("CONSTRAINT_NAME");
                if (cn == null) {
                    continue;
                }
                String name = cn.toString();
                jdbcTemplate.execute("ALTER TABLE orders DROP FOREIGN KEY `" + name.replace("`", "") + "`");
                log.warn(
                        "FK orders.user_id→users supprimée ({}) : les commandes peuvent référencer un userId sans ligne locale inscription.users.",
                        name);
            }
        } catch (Exception e) {
            log.debug("Relaxation FK orders ignorée (non-MySQL, schéma absent, ou déjà sans FK): {}", e.getMessage());
        }
    }
}
