package esprit.inscription.repository;

import esprit.inscription.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);
    
    List<PromoCode> findByActiveTrue();
    
    @Query("SELECT p FROM PromoCode p WHERE p.active = true AND p.validFrom <= :now AND (p.validUntil IS NULL OR p.validUntil >= :now)")
    List<PromoCode> findActivePromoCodes(@Param("now") LocalDateTime now);
    
    @Query("SELECT p FROM PromoCode p WHERE p.active = true AND p.code = :code AND p.validFrom <= :now AND (p.validUntil IS NULL OR p.validUntil >= :now)")
    Optional<PromoCode> findValidPromoCode(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(p) FROM PromoCode p WHERE p.code = :code")
    Long countByCode(@Param("code") String code);
}
