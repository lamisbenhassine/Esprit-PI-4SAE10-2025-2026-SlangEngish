package esprit.inscription.repository;

import esprit.inscription.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    /** When multiple carts exist (e.g. before unique constraint), use the latest one (most recently used). */
    Optional<Cart> findFirstByUserIdOrderByIdDesc(Long userId);
}
