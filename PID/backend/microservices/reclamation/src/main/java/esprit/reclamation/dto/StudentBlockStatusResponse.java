package esprit.reclamation.dto;

public class StudentBlockStatusResponse {

    private boolean blocked;
    private String blockedUntil;
    private String reason;

    public StudentBlockStatusResponse() {
    }

    public StudentBlockStatusResponse(boolean blocked, String blockedUntil, String reason) {
        this.blocked = blocked;
        this.blockedUntil = blockedUntil;
        this.reason = reason;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(String blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
