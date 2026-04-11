package esprit.forum.repository;

import esprit.forum.entity.ForumReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumReportRepository extends JpaRepository<ForumReport, Long> {

    List<ForumReport> findByStatusOrderByCreatedAtDesc(ForumReport.Status status);

    List<ForumReport> findAllByOrderByCreatedAtDesc();

    boolean existsByReporterUserIdAndTargetTypeAndTargetId(
            Long reporterUserId,
            ForumReport.TargetType targetType,
            Long targetId);
}
