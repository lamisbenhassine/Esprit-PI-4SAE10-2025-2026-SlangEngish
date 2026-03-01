package esprit.inscription.repository;

import esprit.inscription.entity.EmailTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTrackingRepository extends JpaRepository<EmailTracking, Long> {

    Optional<EmailTracking> findByEmailId(String emailId);

    List<EmailTracking> findByUserId(Long userId);

    List<EmailTracking> findByCampaignId(Long campaignId);

    List<EmailTracking> findByStatus(EmailTracking.EmailStatus status);

    @Query("SELECT e FROM EmailTracking e WHERE e.campaign.id = :campaignId AND e.status = 'DELIVERED'")
    List<EmailTracking> findDeliveredEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT e FROM EmailTracking e WHERE e.campaign.id = :campaignId AND e.status = 'OPENED'")
    List<EmailTracking> findOpenedEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT e FROM EmailTracking e WHERE e.campaign.id = :campaignId AND e.status = 'CLICKED'")
    List<EmailTracking> findClickedEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT e FROM EmailTracking e WHERE e.campaign.id = :campaignId AND e.status = 'CONVERTED'")
    List<EmailTracking> findConvertedEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(e) FROM EmailTracking e WHERE e.campaign.id = :campaignId")
    Long countTotalEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(e) FROM EmailTracking e WHERE e.campaign.id = :campaignId AND e.status = 'DELIVERED'")
    Long countDeliveredEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(e) FROM EmailTracking e WHERE e.campaign.id = :campaignId AND e.status = 'OPENED'")
    Long countOpenedEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(e) FROM EmailTracking e WHERE e.campaign.id = :campaignId AND e.status = 'CLICKED'")
    Long countClickedEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(e) FROM EmailTracking e WHERE e.campaign.id = :campaignId AND e.status = 'CONVERTED'")
    Long countConvertedEmailsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT e FROM EmailTracking e WHERE e.sentAt BETWEEN :start AND :end")
    List<EmailTracking> findEmailsSentBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(e) FROM EmailTracking e WHERE e.userId = :userId AND e.status = 'OPENED'")
    Long countOpensByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(e) FROM EmailTracking e WHERE e.userId = :userId AND e.status = 'CLICKED'")
    Long countClicksByUser(@Param("userId") Long userId);

    boolean existsByEmailId(String emailId);
}
