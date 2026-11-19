package dev.portfolio.jobtracker.application;

/** Pipeline stages a job application moves through. */
public enum ApplicationStatus {
    WISHLIST,
    APPLIED,
    INTERVIEWING,
    OFFER,
    REJECTED;

    /** True once the company has responded in any way beyond silence. */
    public boolean isResponse() {
        return this == INTERVIEWING || this == OFFER || this == REJECTED;
    }
}
