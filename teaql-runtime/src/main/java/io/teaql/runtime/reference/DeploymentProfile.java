package io.teaql.runtime.reference;

public enum DeploymentProfile {
    DEVELOPMENT("development"), TEST("test"), PRODUCTION("production");

    private final String label;

    DeploymentProfile(String label) { this.label = label; }

    public String label() { return label; }
}
