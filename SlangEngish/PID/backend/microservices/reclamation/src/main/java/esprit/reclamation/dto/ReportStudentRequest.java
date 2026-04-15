package esprit.reclamation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ReportStudentRequest {

    @NotBlank(message = "Report reason is required")
    @Size(max = 500, message = "Report reason is too long")
    private String reportReason;

    public String getReportReason() {
        return reportReason;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
    }
}
