import app.constants.Constants;
import app.Process;
import ui.Ui;

import javax.swing.JOptionPane;

/**
 * DEIHS LIBRARY SYSTEM - ENTRY POINT
 *
 * <p>This is the executable class: run it with {@code java Main}. It does one job -
 * create the application logic and hand it to the user interface. Keeping the
 * launcher this thin is what lets the logic ({@link Process}) and the screens
 * ({@link Ui}) be developed, replaced or tested independently of each other.
 */
public final class Main {

    /** No instances: this class is only a launcher. */
    private Main() {
    }

    public static void main(String[] args) {
        try {
            Process librarySystem = new Process();
            System.out.println("DEIHS Library System");
            System.out.println("  verification codes go to: " + librarySystem.getMailDescription());
            Ui.launch(librarySystem);
        } catch (RuntimeException startupFailure) {
            reportStartupFailure(startupFailure);
        }
    }

    private static void reportStartupFailure(RuntimeException failure) {
        String message = "DEIHS Library System could not start.\n\n"
                + failure.getMessage()
                + "\n\nCheck that MySQL is running, that sql/schema.sql has been applied,"
                + "\nand that " + Constants.CONFIG_FILE + " has the right username and password.";
        System.err.println(message);
        JOptionPane.showMessageDialog(null, message, "Startup Error",
                JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}
