package dev.portfolio.jobtracker.application;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(long id) {
        super("Job application " + id + " not found");
    }
}
