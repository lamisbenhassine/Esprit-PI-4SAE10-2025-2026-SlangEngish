package esprit.forum.service;

import esprit.forum.entity.ForumReport;
import esprit.forum.repository.ForumReportRepository;
import esprit.forum.repository.ForumMessageRepository;
import esprit.forum.repository.ForumTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumReportService {

    private final ForumReportRepository forumReportRepository;
    private final ForumTopicRepository forumTopicRepository;
    private final ForumMessageRepository forumMessageRepository;

    @Transactional
    public ForumReport createReport(Long reporterUserId, ForumReport.TargetType targetType, Long targetId,
            String reason) {
        if (reporterUserId == null || targetType == null || targetId == null) {
            throw new IllegalArgumentException("Missing report fields");
        }
        if (targetType == ForumReport.TargetType.TOPIC) {
            forumTopicRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found"));
        } else {
            forumMessageRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        }
        ForumReport r = new ForumReport();
        r.setReporterUserId(reporterUserId);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        String rsn = reason != null ? reason.strip() : "";
        if (rsn.length() > 500) {
            rsn = rsn.substring(0, 500);
        }
        r.setReason(rsn);
        r.setStatus(ForumReport.Status.OPEN);
        return forumReportRepository.save(r);
    }

    public List<ForumReport> listOpenReports() {
        return forumReportRepository.findByStatusOrderByCreatedAtDesc(ForumReport.Status.OPEN);
    }

    public List<ForumReport> listAllByStatus(ForumReport.Status status) {
        return forumReportRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public ForumReport updateStatus(Long id, ForumReport.Status status) {
        ForumReport r = forumReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        r.setStatus(status);
        return forumReportRepository.save(r);
    }

    public List<ForumReport> listAllReports() {
        return forumReportRepository.findAllByOrderByCreatedAtDesc();
    }
}
