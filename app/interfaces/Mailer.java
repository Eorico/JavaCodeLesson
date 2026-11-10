package app.interfaces;

/** Delivers a message. Implemented by SMTP in production and by the console in dev. */
public interface Mailer {

    void send(String toAddress, String toName, String subject, String body);

    /** Short description of where mail is going, shown in the console at start-up. */
    String describe();
}
