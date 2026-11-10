package app.mail;

import app.interfaces.Mailer;

/** Used when no SMTP account is configured: the code is printed instead of sent. */
public final class ConsoleMailer implements Mailer {

    @Override
    public void send(String toAddress, String toName, String subject, String body) {
        System.out.println();
        System.out.println("=== EMAIL (not sent - mail.enabled is false) ===");
        System.out.println("To:      " + toName + " <" + toAddress + ">");
        System.out.println("Subject: " + subject);
        System.out.println();
        System.out.println(body);
        System.out.println("=== END OF EMAIL ===");
        System.out.println();
    }

    @Override
    public String describe() {
        return "console (no mail is actually sent)";
    }
}
