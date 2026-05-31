package trainfocus.backend.session.domain;

public enum FocusSessionStatus {
    RUNNING,
    PAUSED,
    COMPLETED,
    ABORTED;

    public boolean isEnded() {
        return this == COMPLETED || this == ABORTED;
    }
}
