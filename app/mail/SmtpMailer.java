package app.mail;

import app.config.Config;
import app.exceptions.MailException;
import app.interfaces.Mailer;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * A small SMTP client written directly on a socket, so the program needs no mail
 * library. Port 465 is wrapped in SSL from the first byte; any other port starts
 * in the clear and is upgraded with STARTTLS, which is what port 587 expects.
 */
public final class SmtpMailer implements Mailer {

    private static final int TIMEOUT_MILLIS = 20_000;
    private static final int SSL_ON_CONNECT_PORT = 465;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String fromName;

    public SmtpMailer(String host, int port, String username, String password,
            String fromName) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.fromName = fromName;
    }

    public static SmtpMailer from(Config config) {
        return new SmtpMailer(
                config.get("mail.host", "smtp.gmail.com"),
                config.getInt("mail.port", SSL_ON_CONNECT_PORT),
                config.get("mail.username", ""),
                config.get("mail.password", ""),
                config.get("mail.from.name", "DEIHS Library"));
    }

    @Override
    public String describe() {
        return host + ":" + port;
    }

    @Override
    public void send(String toAddress, String toName, String subject, String body) {
        Socket socket = null;
        try {
            socket = connect();
            BufferedReader in = reader(socket);
            Writer out = writer(socket);

            expect(read(in), 220, "greeting");
            say(out, in, "EHLO deihs-library", 250);

            if (port != SSL_ON_CONNECT_PORT) {
                say(out, in, "STARTTLS", 220);
                socket = upgrade(socket);
                in = reader(socket);
                out = writer(socket);
                say(out, in, "EHLO deihs-library", 250);
            }

            say(out, in, "AUTH LOGIN", 334);
            say(out, in, encode(username), 334);
            say(out, in, encode(password), 235);
            say(out, in, "MAIL FROM:<" + username + ">", 250);
            say(out, in, "RCPT TO:<" + header(toAddress) + ">", 250);
            say(out, in, "DATA", 354);

            write(out, compose(toAddress, toName, subject, body));
            write(out, ".");
            expect(read(in), 250, "message body");

            write(out, "QUIT");
        } catch (IOException networkFailure) {
            throw new MailException("Could not reach the mail server at " + describe()
                    + ". " + networkFailure.getMessage(), networkFailure);
        } finally {
            closeQuietly(socket);
        }
    }

    private Socket connect() throws IOException {
        Socket socket = port == SSL_ON_CONNECT_PORT
                ? SSLSocketFactory.getDefault().createSocket(host, port)
                : new Socket(host, port);
        socket.setSoTimeout(TIMEOUT_MILLIS);
        return socket;
    }

    private Socket upgrade(Socket plain) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket secure = (SSLSocket) factory.createSocket(plain, host, port, true);
        secure.startHandshake();
        secure.setSoTimeout(TIMEOUT_MILLIS);
        return secure;
    }

    private BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private Writer writer(Socket socket) throws IOException {
        return new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
    }

    /** Sends one command and checks the reply code. */
    private void say(Writer out, BufferedReader in, String command, int expected)
            throws IOException {
        write(out, command);
        expect(read(in), expected, firstWord(command));
    }

    private void write(Writer out, String line) throws IOException {
        out.write(line + "\r\n");
        out.flush();
    }

    /** Reads a reply, following the "250-" continuation lines to the last one. */
    private String read(BufferedReader in) throws IOException {
        StringBuilder reply = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            reply.append(line).append('\n');
            if (line.length() < 4 || line.charAt(3) != '-') {
                break;
            }
        }
        if (reply.isEmpty()) {
            throw new IOException("The mail server closed the connection.");
        }
        return reply.toString().trim();
    }

    private void expect(String reply, int expected, String step) throws IOException {
        int actual;
        try {
            actual = Integer.parseInt(reply.substring(0, 3));
        } catch (RuntimeException unparseable) {
            throw new IOException("Unexpected reply at " + step + ": " + reply);
        }
        if (actual != expected) {
            throw new IOException("Mail server refused " + step + ": " + reply);
        }
    }

    private String compose(String toAddress, String toName, String subject, String body) {
        String sentAt = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        return "From: \"" + header(fromName) + "\" <" + username + ">\r\n"
                + "To: \"" + header(toName) + "\" <" + header(toAddress) + ">\r\n"
                + "Subject: " + header(subject) + "\r\n"
                + "Date: " + sentAt + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=\"UTF-8\"\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "\r\n"
                + Base64.getMimeEncoder().encodeToString(
                        body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Strips the characters that would let someone smuggle extra headers into the
     * message by putting a line break in their own name or email address.
     */
    private String header(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\"]", " ").trim();
    }

    private String encode(String value) {
        return Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String firstWord(String command) {
        int space = command.indexOf(' ');
        String word = space < 0 ? command : command.substring(0, space);
        // Never echo the base64 of a username or password back to the user.
        return word.length() > 12 ? "authentication" : word;
    }

    private void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // nothing useful can be done at this point
            }
        }
    }
}

