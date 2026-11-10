package app.enums;

/** Why a one-time code was sent. */
public enum CodePurpose {
    SIGN_UP("confirm your email address"),
    PASSWORD_RESET("reset your password");

    private final String description;

    CodePurpose(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
