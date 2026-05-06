package esprit.forum.controller;

import esprit.forum.entity.ForumReport;
import esprit.forum.service.ForumReportService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumReportController {

    private final ForumReportService forumReportService;

    @PostMapping
    public ResponseEntity<?> report(@RequestBody ReportRequest req) {
        try {
            ForumReport r = forumReportService.createReport(
                    req.getReporterUserId(),
                    req.getTargetType(),
                    req.getTargetId(),
                    req.getReason());
            return ResponseEntity.status(HttpStatus.CREATED).body(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/open")
    public ResponseEntity<List<ForumReport>> listOpen() {
        return ResponseEntity.ok(forumReportService.listOpenReports());
    }

    @GetMapping
    public ResponseEntity<List<ForumReport>> listAll() {
        return ResponseEntity.ok(forumReportService.listAllReports());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        try {
            return ResponseEntity.ok(forumReportService.updateStatus(id, req.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Data
    public static class ReportRequest {
        private Long reporterUserId;
        private ForumReport.TargetType targetType;
        private Long targetId;
        private String reason;
    }

    @Data
    public static class StatusRequest {
        private ForumReport.Status status;
    }
}
