package esprit.inscription.repository;

import esprit.inscription.entity.EmailCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {

    List<EmailCampaign> findByStatus(EmailCampaign.CampaignStatus status);

    Page<EmailCampaign> findByStatusOrderByCreatedAtDesc(EmailCampaign.CampaignStatus status, Pageable pageable);

    List<EmailCampaign> findByCategory(EmailCampaign.CampaignCategory category);

    List<EmailCampaign> findByTargetLevel(String targetLevel);

    @Query("SELECT c FROM EmailCampaign c WHERE c.scheduledAt BETWEEN :start AND :end ORDER BY c.scheduledAt ASC")
    List<EmailCampaign> findScheduledCampaignsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM EmailCampaign c WHERE c.status = 'SCHEDULED' AND c.scheduledAt <= :now")
    List<EmailCampaign> findCampaignsReadyToSend(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM EmailCampaign c WHERE c.status IN ('SENDING', 'SCHEDULED') AND c.targetLevel = :level")
    List<EmailCampaign> findActiveCampaignsByLevel(@Param("level") String level);

    @Query("SELECT COUNT(c) FROM EmailCampaign c WHERE c.status = :status AND c.createdAt >= :since")
    Long countByStatusSince(@Param("status") EmailCampaign.CampaignStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT c FROM EmailCampaign c WHERE c.name LIKE %:keyword% OR c.description LIKE %:keyword%")
    Page<EmailCampaign> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM EmailCampaign c WHERE c.status = 'SENDING' AND c.sentCount < c.totalRecipients")
    List<EmailCampaign> findIncompleteSendingCampaigns();

    @Query("SELECT DISTINCT c.targetLevel FROM EmailCampaign c WHERE c.targetLevel IS NOT NULL")
    List<String> findAllTargetLevels();

    boolean existsByName(String name);
}
