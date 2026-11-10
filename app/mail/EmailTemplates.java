package app.mail;

import app.constants.Constants;
import app.enums.CodePurpose;
import app.models.User;

/** The wording of the two messages the system sends. */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static String subjectFor(CodePurpose purpose) {
        return purpose == CodePurpose.SIGN_UP
                ? "Your DEIHS Library verification code"
                : "Your DEIHS Library password reset code";
    }

    public static String bodyFor(CodePurpose purpose, User user, String code) {
        String opening = purpose == CodePurpose.SIGN_UP
                ? "Welcome to the DEIHS Library. Use this code to " + purpose.getDescription()
                : "We received a request to " + purpose.getDescription();
        String closing = purpose == CodePurpose.SIGN_UP
                ? "If you did not create this account, you can ignore this email."
                : "If you did not ask for this, your password has not been changed.";
        return "Hi " + user.getFullName() + ","
                + "\n\n" + opening + ":"
                + "\n\n        " + code
                + "\n\nThe code expires in " + Constants.CODE_VALID_MINUTES + " minutes."
                + "\n" + closing
                + "\n\n-- \nDEIHS Library System";
    }
}
