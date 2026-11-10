package app.models;

import app.enums.Role;
import app.interfaces.Identifiable;
import app.util.Validator;

import java.util.Locale;

/** A registered account. The username is the key, so it never changes. */
public final class User implements Identifiable<String> {

    private final String username;
    private final Role role;
    private String fullName;
    private String email;
    private String passwordHash;
    private boolean verified;

    public User(String username, String fullName, String email, Role role) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    @Override
    public String id() {
        return username;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    /** False until the member types the code that was emailed to them. */
    public boolean isVerified() {
        return verified;
    }

    public boolean isLibrarian() {
        return role == Role.LIBRARIAN;
    }

    /** First letter of the display name, used by the UI avatar. */
    public String getInitial() {
        String source = fullName == null || fullName.isBlank() ? username : fullName;
        return source.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    /** The address with most of the local part hidden, safe to show on screen. */
    public String getMaskedEmail() {
        return Validator.maskEmail(email);
    }

    /** Only ever the salted hash; the plain password is never held on this object. */
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setProfile(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    @Override
    public String toString() {
        return fullName + " (@" + username + ")";
    }
}
