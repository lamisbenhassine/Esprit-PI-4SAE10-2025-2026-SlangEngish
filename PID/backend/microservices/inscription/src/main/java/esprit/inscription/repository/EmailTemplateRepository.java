package esprit.inscription.repository;

import esprit.inscription.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    List<EmailTemplate> findByCategory(EmailTemplate.TemplateCategory category);

    List<EmailTemplate> findByStatus(EmailTemplate.TemplateStatus status);

    Optional<EmailTemplate> findByName(String name);

    @Query("SELECT t FROM EmailTemplate t WHERE t.status = 'ACTIVE' AND t.category = :category")
    List<EmailTemplate> findActiveTemplatesByCategory(@Param("category") EmailTemplate.TemplateCategory category);

    @Query("SELECT t FROM EmailTemplate t WHERE t.name LIKE %:keyword% OR t.displayName LIKE %:keyword%")
    List<EmailTemplate> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT t.category FROM EmailTemplate t WHERE t.status = 'ACTIVE'")
    List<EmailTemplate.TemplateCategory> findAllActiveCategories();

    @Query("SELECT COUNT(t) FROM EmailTemplate t WHERE t.category = :category AND t.status = 'ACTIVE'")
    Long countActiveTemplatesByCategory(@Param("category") EmailTemplate.TemplateCategory category);

    boolean existsByName(String name);
}
