package app.services;

import app.constants.Constants;
import app.enums.CodePurpose;
import app.enums.Role;
import app.exceptions.MailException;
import app.interfaces.CodeRepository;
import app.interfaces.Mailer;
import app.interfaces.PasswordEncoder;
import app.interfaces.UserRepository;
import app.mail.EmailTemplates;
import app.models.Result;
import app.models.User;
import app.models.VerificationCode;
import app.security.CodeFactory;
import app.util.Validator;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/** Sign up, email verification, sign in and password recovery. */
public final class AuthService {

    private final UserRepository users;
    private final CodeRepository codes;
    private final PasswordEncoder encoder;
    private final Mailer mailer;
    private final CodeFactory codeFactory = new CodeFactory();

    public AuthService(UserRepository users, CodeRepository codes, PasswordEncoder encoder,
            Mailer mailer) {
        this.users = users;
        this.codes = codes;
        this.encoder = encoder;
        this.mailer = mailer;
    }

    /** Creates an unverified account and emails the first code. */
    public Result<User> register(String username, String fullName, String email,
            String password, String confirmation) {
        String key = Validator.username(username);
        if (users.findById(key).isPresent()) {
            return Result.fail("That username is already taken.");
        }
        String name = Validator.text(fullName, "Full name", 2, 120);
        String mail = Validator.email(email);
        if (users.existsByEmail(mail)) {
            return Result.fail("That email address is already registered.");
        }
        Validator.password(password);
        Validator.confirmed(password, confirmation);

        User user = new User(key, name, mail, Role.MEMBER);
        user.setPasswordHash(encoder.encode(password));
        user.setVerified(false);
        users.save(user);

        Result<String> sent = sendCode(user, CodePurpose.SIGN_UP);
        if (!sent.isSuccess()) {
            return Result.fail(sent.getMessage());
        }
        return Result.ok("Account created. We sent a code to " + user.getMaskedEmail() + ".",
                user);
    }

    /** Checks a sign-up code and opens the account. */
    public Result<User> verify(String username, String rawCode) {
        Result<User> redeemed = redeem(username, rawCode, CodePurpose.SIGN_UP);
        if (!redeemed.isSuccess()) {
            return redeemed;
        }
        User user = redeemed.getValue().orElseThrow();
        user.setVerified(true);
        users.save(user);
        return Result.ok("Email confirmed. You can sign in now.", user);
    }

    public Result<User> authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Result.fail("Please enter your username and password.");
        }
        Optional<User> found = users.findById(normalise(username));
        if (found.isEmpty() || !encoder.matches(password, found.get().getPasswordHash())) {
            return Result.fail("Incorrect username or password.");
        }
        User user = found.get();
        if (!user.isVerified()) {
            return Result.fail("Please confirm your email address first. "
                    + "We sent a code to " + user.getMaskedEmail() + ".");
        }
        return Result.ok("Signed in as " + user.getFullName() + ".", user);
    }

    /** Step 1 of recovery: email a fresh code. */
    public Result<String> requestPasswordReset(String username) {
        Optional<User> found = users.findById(normalise(username == null ? "" : username));
        if (found.isEmpty()) {
            return Result.fail("We could not find that account.");
        }
        return sendCode(found.get(), CodePurpose.PASSWORD_RESET);
    }

    /** Step 2 of recovery: check the code and set the new password. */
    public Result<User> resetPassword(String username, String rawCode, String newPassword,
            String confirmation) {
        Validator.password(newPassword);
        Validator.confirmed(newPassword, confirmation);
        Result<User> redeemed = redeem(username, rawCode, CodePurpose.PASSWORD_RESET);
        if (!redeemed.isSuccess()) {
            return redeemed;
        }
        User user = redeemed.getValue().orElseThrow();
        user.setPasswordHash(encoder.encode(newPassword));
        // Resetting by email proves the address works, so the account is confirmed too.
        user.setVerified(true);
        users.save(user);
        return Result.ok("Password updated. You can sign in now.", user);
    }

    /** Sends another code for whichever flow the member is in the middle of. */
    public Result<String> resend(String username, CodePurpose purpose) {
        Optional<User> found = users.findById(normalise(username == null ? "" : username));
        if (found.isEmpty()) {
            return Result.fail("We could not find that account.");
        }
        return sendCode(found.get(), purpose);
    }

    public int countUsers() {
        return users.count();
    }

    /** Issues, stores and emails a code. The plain number is never written down. */
    private Result<String> sendCode(User user, CodePurpose purpose) {
        String plainCode = codeFactory.next();
        codes.issue(user.getUsername(), purpose, encoder.encode(plainCode),
                LocalDateTime.now().plusMinutes(Constants.CODE_VALID_MINUTES));
        try {
            mailer.send(user.getEmail(), user.getFullName(),
                    EmailTemplates.subjectFor(purpose),
                    EmailTemplates.bodyFor(purpose, user, plainCode));
        } catch (MailException undelivered) {
            return Result.fail("We could not send the email. " + undelivered.getMessage());
        }
        return Result.ok("A " + Constants.CODE_LENGTH + " digit code is on its way to "
                + user.getMaskedEmail() + ".", user.getMaskedEmail());
    }

    /**
     * The one place a code is checked, shared by sign-up confirmation and password
     * recovery. Expired and over-guessed codes are burned rather than left lying
     * around for someone to keep trying.
     */
    private Result<User> redeem(String username, String rawCode, CodePurpose purpose) {
        Optional<User> found = users.findById(normalise(username == null ? "" : username));
        if (found.isEmpty()) {
            return Result.fail("We could not find that account.");
        }
        User user = found.get();
        String digits = Validator.code(rawCode);

        Optional<VerificationCode> active = codes.findActive(user.getUsername(), purpose);
        if (active.isEmpty()) {
            return Result.fail("No code is waiting for you. Ask for a new one.");
        }
        VerificationCode code = active.get();
        if (code.isExpired()) {
            codes.consume(code);
            return Result.fail("That code has expired. Ask for a new one.");
        }
        if (code.isExhausted()) {
            codes.consume(code);
            return Result.fail("Too many wrong tries. Ask for a new code.");
        }
        if (!encoder.matches(digits, code.getCodeHash())) {
            codes.recordAttempt(code);
            int left = Math.max(0, Constants.MAX_CODE_ATTEMPTS - code.getAttempts());
            return Result.fail(left == 0
                    ? "That code is not right, and you are out of tries. Ask for a new one."
                    : "That code is not right. " + left + " "
                            + (left == 1 ? "try" : "tries") + " left.");
        }
        codes.consume(code);
        return Result.ok("Code accepted.", user);
    }

    private String normalise(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
