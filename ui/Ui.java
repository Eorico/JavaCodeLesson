package ui;

import app.Process;
import app.constants.Constants;
import app.enums.CodePurpose;
import app.models.Book;
import app.models.Result;
import app.models.Statistics;
import app.models.User;
import app.util.Lazy;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * DEIHS LIBRARY SYSTEM - USER INTERFACE
 *
 * <p>A Swing front end in a blue / yellow / white theme. This class only draws
 * widgets and forwards what the user typed to {@link Process}; it contains no
 * library rules of its own. All feedback shown on screen is the message that the
 * logic layer returned.
 *
 * <p>Reusable building blocks ({@link RoundButton}, {@link HintField},
 * {@link Card}, {@link StatTile}, {@link BookTableModel}) are declared once at the
 * bottom of the file and shared by every screen, so no styling code is repeated.
 */
public final class Ui extends JFrame {

    /* ===================== THEME ===================== */

    private static final Color BLUE_DEEP = new Color(0x0B2E63);
    private static final Color BLUE = new Color(0x1457B8);
    private static final Color BLUE_HOVER = new Color(0x1049A0);
    private static final Color BLUE_SOFT = new Color(0xE8F0FC);
    private static final Color YELLOW = new Color(0xFFC227);
    private static final Color YELLOW_HOVER = new Color(0xECAE0C);
    private static final Color WHITE = Color.WHITE;
    private static final Color SURFACE = new Color(0xF5F8FD);
    private static final Color INK = new Color(0x16233A);
    private static final Color MUTED = new Color(0x6E7E95);
    private static final Color LINE = new Color(0xDCE5F2);
    private static final Color SUCCESS = new Color(0x1C7A52);
    private static final Color DANGER = new Color(0xB3331F);

    private static final String FAMILY = pickFontFamily();

    /* ===================== CARD NAMES ===================== */

    private static final String SCREEN_AUTH = "auth";
    private static final String SCREEN_APP = "app";

    private static final String FORM_LOGIN = "login";
    private static final String FORM_SIGNUP = "signup";
    private static final String FORM_VERIFY = "verify";
    private static final String FORM_FORGOT = "forgot";
    private static final String FORM_RESET = "reset";

    private static final String VIEW_DASHBOARD = "Dashboard";
    private static final String VIEW_CATALOGUE = "Catalogue";
    private static final String VIEW_MY_BOOKS = "My Books";
    private static final String VIEW_ADD_BOOK = "Add Book";

    /* ===================== STATE ===================== */

    private final Process process;

    private final JPanel screens = new JPanel(new CardStack());
    private final JPanel authForms = new JPanel(new CardStack());
    private final JPanel views = new JPanel(new CardStack());

    /* login form */
    private final HintField loginUsername = new HintField("Username", 18);
    private final HintPasswordField loginPassword = new HintPasswordField("Password", 18);
    private final MessageBar loginMessage = new MessageBar(286);

    /* sign-up form */
    private final HintField signUpName = new HintField("Juan Dela Cruz", 18);
    private final HintField signUpUsername = new HintField("juandelacruz", 18);
    private final HintField signUpEmail = new HintField("juan@email.com", 18);
    private final HintPasswordField signUpPassword = new HintPasswordField("At least 6 characters", 18);
    private final HintPasswordField signUpConfirm = new HintPasswordField("Repeat password", 18);
    private final RoundButton signUpSubmit =
            new RoundButton("Create Account", RoundButton.Style.PRIMARY);
    private final MessageBar signUpMessage = new MessageBar(286);

    /* email verification form, shared by sign-up and password recovery */
    private final JLabel verifyHeading = new JLabel("Check your email");
    private final JLabel verifyBlurb = new JLabel();
    private final HintField verifyCode = new HintField("6 digit code", 18);
    private final RoundButton verifySubmit =
            new RoundButton("Confirm Email", RoundButton.Style.PRIMARY);
    private final JButton verifyResend = linkButton("Send it again");
    private final MessageBar verifyMessage = new MessageBar(286);

    /** Who the emailed code belongs to, and which flow it is part of. */
    private String pendingUsername = "";
    private CodePurpose pendingPurpose = CodePurpose.SIGN_UP;

    /* forgot-password form */
    private final HintField forgotUsername = new HintField("Username", 18);
    private final RoundButton forgotSend =
            new RoundButton("Email Me a Code", RoundButton.Style.PRIMARY);
    private final MessageBar forgotMessage = new MessageBar(286);

    /* choose-new-password form, reached after the reset code is accepted */
    private final HintField resetCode = new HintField("6 digit code", 18);
    private final HintPasswordField resetPassword = new HintPasswordField("New password", 18);
    private final HintPasswordField resetConfirm = new HintPasswordField("Repeat new password", 18);
    private final RoundButton resetSubmit =
            new RoundButton("Reset Password", RoundButton.Style.PRIMARY);
    private final JButton resetResend = linkButton("Send it again");
    private final JLabel resetBlurb = new JLabel();
    private final MessageBar resetMessage = new MessageBar(286);

    /* application shell */
    private final Avatar avatar = new Avatar();
    private final JLabel accountName = new JLabel();
    private final JLabel accountRole = new JLabel();
    private final MessageBar appMessage = new MessageBar(720);
    private final List<NavButton> navButtons = new ArrayList<>();

    /* catalogue + my books */
    private final HintField searchField = new HintField("Search by title, author, category or ID", 24);
    private final BookTableModel catalogueModel = new BookTableModel();
    private final BookTableModel myBooksModel = new BookTableModel();
    private final JTable catalogueTable = styledTable(catalogueModel);
    private final JTable myBooksTable = styledTable(myBooksModel);
    private final RoundButton removeButton = new RoundButton("Remove", RoundButton.Style.DANGER);

    /* add book */
    private final HintField bookTitle = new HintField("Book title", 20);
    private final HintField bookAuthor = new HintField("Author name", 20);
    private final JComboBox<String> bookCategory = new JComboBox<>();

    /* dashboard */
    private final StatTile tileTotal = new StatTile("Total Books", BLUE);
    private final StatTile tileAvailable = new StatTile("Available", SUCCESS);
    private final StatTile tileBorrowed = new StatTile("On Loan", YELLOW_HOVER);
    private final StatTile tileMine = new StatTile("Borrowed by You", BLUE_DEEP);
    private final JLabel greeting = new JLabel();
    private final BookTableModel dashboardModel = new BookTableModel();
    private final JTable dashboardTable = styledTable(dashboardModel);

    /*
     * Lazy screens. A view is described here but not built until it is first opened,
     * and its contents are reloaded only when something has actually changed since
     * the last time it was looked at.
     */
    private final Map<String, Lazy<JComponent>> viewBuilders = new LinkedHashMap<>();
    private final Set<String> builtViews = new HashSet<>();
    private final Set<String> staleViews = new HashSet<>();
    private String currentView = VIEW_DASHBOARD;

    /* Catalogue paging: rows arrive a page at a time as the table is scrolled. */
    private final JLabel catalogueStatus = new JLabel();
    private String catalogueQuery = "";
    private int catalogueLoaded;
    private int catalogueTotal;
    private boolean catalogueLoading;

    /** True while the window is narrow enough to need the compact layout. */
    private boolean compact;

    /* Pieces the responsive pass rearranges, kept so they can be found again. */
    private JComponent brandPanel;
    private JPanel sidebar;
    private JLabel sidebarCaption;
    private JLabel sidebarHint;
    private JComponent sidebarWordmark;
    private JPanel tiles;

    /* ===================== BOOTSTRAP ===================== */

    public Ui(Process process) {
        this.process = process;
        buildWindow();
    }

    /** Applies the system look and feel and shows the window on the Swing thread. */
    public static void launch(Process process) {
        installLookAndFeel();
        SwingUtilities.invokeLater(() -> new Ui(process).setVisible(true));
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
            // The cross-platform default is perfectly usable.
        }
        UIManager.put("ToolTip.background", WHITE);
        UIManager.put("ToolTip.foreground", INK);
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("Panel.background", WHITE);
        UIManager.put("OptionPane.messageFont", new Font(FAMILY, Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font(FAMILY, Font.BOLD, 13));
    }

    private static String pickFontFamily() {
        List<String> available = List.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames());
        for (String candidate : List.of("Segoe UI", "Inter", "Roboto", "Helvetica Neue")) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    private void buildWindow() {
        setTitle("DEIHS Library System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(640, 560));
        setSize(new Dimension(1180, 740));
        setLocationRelativeTo(null);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                applyResponsiveLayout();
            }
        });

        screens.add(buildAuthScreen(), SCREEN_AUTH);
        screens.add(buildAppScreen(), SCREEN_APP);
        setContentPane(screens);
        show(screens, SCREEN_AUTH);
        show(authForms, FORM_LOGIN);
        applyResponsiveLayout();

        SwingUtilities.invokeLater(loginUsername::requestFocusInWindow);

        // Lazy, but not lazy to a fault: open the database in the background while
        // the member is still typing, so signing in does not pay for connecting.
        // A failure here stays quiet, because it is reported properly by whichever
        // action actually needs the database first.
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                process.warmUp();
                return null;
            }
        }.execute();
    }

    /* ===================================================================== */
    /* AUTHENTICATION SCREEN                                                 */
    /* ===================================================================== */

    private JComponent buildAuthScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(SURFACE);
        brandPanel = buildBrandPanel();
        screen.add(brandPanel, BorderLayout.WEST);

        authForms.setOpaque(false);
        authForms.setBorder(new EmptyBorder(28, 48, 28, 48));
        authForms.add(wrapScrollable(buildLoginForm()), FORM_LOGIN);
        authForms.add(wrapScrollable(buildSignUpForm()), FORM_SIGNUP);
        authForms.add(wrapScrollable(buildVerifyForm()), FORM_VERIFY);
        authForms.add(wrapScrollable(buildForgotForm()), FORM_FORGOT);
        authForms.add(wrapScrollable(buildResetForm()), FORM_RESET);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(SURFACE);
        right.add(authForms, BorderLayout.CENTER);
        screen.add(right, BorderLayout.CENTER);
        return screen;
    }

    /** The blue branding column on the left of every authentication form. */
    private JComponent buildBrandPanel() {
        GradientPanel brand = new GradientPanel(BLUE_DEEP, BLUE);
        brand.setPreferredSize(new Dimension(420, 0));
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(60, 46, 46, 46));

        JPanel logo = new JPanel(new BorderLayout(14, 0));
        logo.setOpaque(false);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.add(new Monogram(), BorderLayout.WEST);
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(label("DEIHS", Font.BOLD, 26, WHITE));
        words.add(label("LIBRARY SYSTEM", Font.BOLD, 13, YELLOW));
        logo.add(words, BorderLayout.CENTER);
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        brand.add(logo);
        brand.add(Box.createVerticalGlue());

        brand.add(label("Every book,\none shelf away.", Font.BOLD, 30, WHITE));
        brand.add(Box.createVerticalStrut(14));
        brand.add(label("Browse the catalogue, borrow in a click\nand keep track of what"
                + " you still have\nto return.", Font.PLAIN, 14, new Color(0xCBDCF5)));
        brand.add(Box.createVerticalStrut(30));

        for (String feature : List.of("Live catalogue with instant search",
                "Seven-day loans with due-date tracking",
                "Secure accounts and password recovery")) {
            brand.add(bullet(feature));
            brand.add(Box.createVerticalStrut(12));
        }

        brand.add(Box.createVerticalGlue());
        brand.add(label("Demo librarian  ·  admin / admin123", Font.PLAIN, 12,
                new Color(0x9FBDE8)));
        return brand;
    }

    private JComponent bullet(String text) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.add(new Dot(YELLOW), BorderLayout.WEST);
        row.add(label(text, Font.PLAIN, 13, WHITE), BorderLayout.CENTER);
        return row;
    }

    private JComponent buildLoginForm() {
        Card card = new Card();
        card.add(label("Welcome back", Font.BOLD, 24, INK));
        card.add(Box.createVerticalStrut(6));
        card.add(label("Sign in to continue to your library.", Font.PLAIN, 13, MUTED));
        card.add(Box.createVerticalStrut(22));
        card.add(loginMessage);

        card.add(field("Username", loginUsername));
        card.add(field("Password", loginPassword));

        RoundButton signIn = new RoundButton("Sign In", RoundButton.Style.PRIMARY);
        signIn.addActionListener(event -> doSignIn());
        loginPassword.addActionListener(event -> doSignIn());
        loginUsername.addActionListener(event -> loginPassword.requestFocusInWindow());

        JButton forgot = linkButton("Forgot password?");
        forgot.addActionListener(event -> openForm(FORM_FORGOT));
        card.add(row(forgot));
        card.add(Box.createVerticalStrut(14));
        card.add(stretch(signIn));
        card.add(Box.createVerticalStrut(18));
        card.add(divider());
        card.add(Box.createVerticalStrut(14));

        JButton createAccount = linkButton("Create one");
        createAccount.addActionListener(event -> openForm(FORM_SIGNUP));
        JPanel switcher = row(label("New to DEIHS Library?", Font.PLAIN, 13, MUTED), createAccount);
        card.add(switcher);
        return card;
    }

    private JComponent buildSignUpForm() {
        Card card = new Card();
        card.add(label("Create your account", Font.BOLD, 24, INK));
        card.add(Box.createVerticalStrut(6));
        card.add(label("It takes less than a minute.", Font.PLAIN, 13, MUTED));
        card.add(Box.createVerticalStrut(20));
        card.add(signUpMessage);

        card.add(field("Full name", signUpName));
        card.add(pair(field("Username", signUpUsername), field("Email address", signUpEmail)));
        card.add(pair(field("Password", signUpPassword), field("Confirm password", signUpConfirm)));
        card.add(label("We email a 6 digit code to confirm your address.",
                Font.PLAIN, 12, MUTED));

        signUpSubmit.addActionListener(event -> doSignUp());
        signUpConfirm.addActionListener(event -> doSignUp());
        card.add(Box.createVerticalStrut(14));
        card.add(stretch(signUpSubmit));
        card.add(Box.createVerticalStrut(16));
        card.add(divider());
        card.add(Box.createVerticalStrut(14));

        JButton back = linkButton("Sign in");
        back.addActionListener(event -> openForm(FORM_LOGIN));
        card.add(row(label("Already have an account?", Font.PLAIN, 13, MUTED), back));
        return card;
    }

    /** Step two of signing up: type the code that was emailed. */
    private JComponent buildVerifyForm() {
        Card card = new Card();
        verifyHeading.setFont(new Font(FAMILY, Font.BOLD, 24));
        verifyHeading.setForeground(INK);
        verifyHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(verifyHeading);
        card.add(Box.createVerticalStrut(6));

        verifyBlurb.setFont(new Font(FAMILY, Font.PLAIN, 13));
        verifyBlurb.setForeground(MUTED);
        verifyBlurb.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(verifyBlurb);
        card.add(Box.createVerticalStrut(20));
        card.add(verifyMessage);

        styleCodeField(verifyCode);
        card.add(field("Verification code", verifyCode));

        verifySubmit.addActionListener(event -> doVerify());
        verifyCode.addActionListener(event -> doVerify());
        card.add(Box.createVerticalStrut(4));
        card.add(stretch(verifySubmit));
        card.add(Box.createVerticalStrut(14));

        verifyResend.addActionListener(event -> doResend(verifyMessage, verifyResend));
        card.add(row(label("Did not get it?", Font.PLAIN, 13, MUTED), verifyResend));
        card.add(Box.createVerticalStrut(14));
        card.add(divider());
        card.add(Box.createVerticalStrut(14));

        JButton back = linkButton("Back to sign in");
        back.addActionListener(event -> openForm(FORM_LOGIN));
        card.add(row(back));
        return card;
    }

    /** Step one of recovery: say who you are so a code can be sent. */
    private JComponent buildForgotForm() {
        Card card = new Card();
        card.add(label("Reset your password", Font.BOLD, 24, INK));
        card.add(Box.createVerticalStrut(6));
        card.add(label("We will email a code to the address on your account.",
                Font.PLAIN, 13, MUTED));
        card.add(Box.createVerticalStrut(20));
        card.add(forgotMessage);

        card.add(field("Username", forgotUsername));

        forgotSend.addActionListener(event -> doRequestReset());
        forgotUsername.addActionListener(event -> doRequestReset());
        card.add(Box.createVerticalStrut(4));
        card.add(stretch(forgotSend));
        card.add(Box.createVerticalStrut(16));
        card.add(divider());
        card.add(Box.createVerticalStrut(14));

        JButton back = linkButton("Back to sign in");
        back.addActionListener(event -> openForm(FORM_LOGIN));
        card.add(row(back));
        return card;
    }

    /** Step two of recovery: the emailed code plus the new password. */
    private JComponent buildResetForm() {
        Card card = new Card();
        card.add(label("Choose a new password", Font.BOLD, 24, INK));
        card.add(Box.createVerticalStrut(6));

        resetBlurb.setFont(new Font(FAMILY, Font.PLAIN, 13));
        resetBlurb.setForeground(MUTED);
        resetBlurb.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(resetBlurb);
        card.add(Box.createVerticalStrut(20));
        card.add(resetMessage);

        styleCodeField(resetCode);
        card.add(field("Verification code", resetCode));
        card.add(pair(field("New password", resetPassword),
                field("Confirm password", resetConfirm)));

        resetSubmit.addActionListener(event -> doResetPassword());
        resetConfirm.addActionListener(event -> doResetPassword());
        card.add(Box.createVerticalStrut(4));
        card.add(stretch(resetSubmit));
        card.add(Box.createVerticalStrut(14));

        resetResend.addActionListener(event -> doResend(resetMessage, resetResend));
        card.add(row(label("Did not get it?", Font.PLAIN, 13, MUTED), resetResend));
        card.add(Box.createVerticalStrut(14));
        card.add(divider());
        card.add(Box.createVerticalStrut(14));

        JButton back = linkButton("Back to sign in");
        back.addActionListener(event -> openForm(FORM_LOGIN));
        card.add(row(back));
        return card;
    }

    /** Codes read better widely spaced and centred. */
    private static void styleCodeField(HintField input) {
        input.setFont(new Font(FAMILY, Font.BOLD, 20));
        input.setHorizontalAlignment(SwingConstants.CENTER);
    }

    /* ===================================================================== */
    /* APPLICATION SCREEN                                                    */
    /* ===================================================================== */

    private JComponent buildAppScreen() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(SURFACE);
        shell.add(buildSidebar(), BorderLayout.WEST);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(SURFACE);
        body.add(buildTopBar(), BorderLayout.NORTH);

        views.setOpaque(false);
        views.setBorder(new EmptyBorder(20, 24, 24, 24));
        // Described now, constructed the first time each one is opened.
        viewBuilders.put(VIEW_DASHBOARD, Lazy.of(this::buildDashboardView));
        viewBuilders.put(VIEW_CATALOGUE, Lazy.of(this::buildCatalogueView));
        viewBuilders.put(VIEW_MY_BOOKS, Lazy.of(this::buildMyBooksView));
        viewBuilders.put(VIEW_ADD_BOOK, Lazy.of(this::buildAddBookView));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        JPanel banner = new JPanel(new BorderLayout());
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(16, 24, 0, 24));
        banner.add(appMessage, BorderLayout.CENTER);
        content.add(banner, BorderLayout.NORTH);
        content.add(views, BorderLayout.CENTER);

        body.add(content, BorderLayout.CENTER);
        shell.add(body, BorderLayout.CENTER);
        return shell;
    }

    private JComponent buildSidebar() {
        sidebar = new GradientPanel(BLUE_DEEP, BLUE);
        sidebar.setPreferredSize(new Dimension(248, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(26, 18, 22, 18));

        JPanel logo = new JPanel(new BorderLayout(12, 0));
        logo.setOpaque(false);
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        logo.add(new Monogram(), BorderLayout.WEST);
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(label("DEIHS", Font.BOLD, 18, WHITE));
        words.add(label("LIBRARY", Font.BOLD, 10, YELLOW));
        sidebarWordmark = words;
        logo.add(words, BorderLayout.CENTER);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(30));

        sidebarCaption = label("MENU", Font.BOLD, 10, new Color(0x8FB2E4));
        sidebarCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sidebarCaption);
        sidebar.add(Box.createVerticalStrut(10));

        ButtonGroup group = new ButtonGroup();
        for (String view : List.of(VIEW_DASHBOARD, VIEW_CATALOGUE, VIEW_MY_BOOKS, VIEW_ADD_BOOK)) {
            NavButton button = new NavButton(view);
            button.addActionListener(event -> openView(view));
            group.add(button);
            navButtons.add(button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(6));
        }

        sidebar.add(Box.createVerticalGlue());
        sidebarHint = label("Loans run for " + Constants.LOAN_PERIOD_DAYS + " days.",
                Font.PLAIN, 11, new Color(0x9FBDE8));
        sidebarHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sidebarHint);
        return sidebar;
    }

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(14, 24, 14, 24)));

        JPanel account = new JPanel(new BorderLayout(12, 0));
        account.setOpaque(false);
        account.add(avatar, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        accountName.setFont(new Font(FAMILY, Font.BOLD, 15));
        accountName.setForeground(INK);
        accountRole.setFont(new Font(FAMILY, Font.PLAIN, 12));
        accountRole.setForeground(MUTED);
        text.add(accountName);
        text.add(accountRole);
        account.add(text, BorderLayout.CENTER);
        bar.add(account, BorderLayout.WEST);

        RoundButton signOut = new RoundButton("Sign Out", RoundButton.Style.GHOST);
        signOut.addActionListener(event -> doSignOut());
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(signOut, BorderLayout.EAST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildDashboardView() {
        JPanel view = new JPanel(new BorderLayout(0, 18));
        view.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        greeting.setFont(new Font(FAMILY, Font.BOLD, 22));
        greeting.setForeground(INK);
        header.add(greeting, BorderLayout.NORTH);
        header.add(label("Here is what the shelves look like today.", Font.PLAIN, 13, MUTED),
                BorderLayout.CENTER);

        tiles = new JPanel(new GridLayout(compact ? 2 : 1, compact ? 2 : 4, 14, 14));
        tiles.setOpaque(false);
        tiles.add(tileTotal);
        tiles.add(tileAvailable);
        tiles.add(tileBorrowed);
        tiles.add(tileMine);

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(tiles, BorderLayout.CENTER);
        view.add(top, BorderLayout.NORTH);

        Panel surface = new Panel();
        surface.setLayout(new BorderLayout(0, 12));
        surface.add(sectionTitle("Books you currently have", "Return them before the due date."),
                BorderLayout.NORTH);
        surface.add(tableScroll(dashboardTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        RoundButton browse = new RoundButton("Browse Catalogue", RoundButton.Style.PRIMARY);
        browse.addActionListener(event -> openView(VIEW_CATALOGUE));
        RoundButton mine = new RoundButton("Open My Books", RoundButton.Style.GHOST);
        mine.addActionListener(event -> openView(VIEW_MY_BOOKS));
        JPanel buttons = row(browse, mine);
        actions.add(buttons, BorderLayout.WEST);
        surface.add(actions, BorderLayout.SOUTH);

        view.add(surface, BorderLayout.CENTER);
        return view;
    }

    private JComponent buildCatalogueView() {
        Panel view = new Panel();
        view.setLayout(new BorderLayout(0, 14));
        view.add(sectionTitle("Catalogue", "Select a row, then borrow or return it."),
                BorderLayout.NORTH);

        RoundButton search = new RoundButton("Search", RoundButton.Style.PRIMARY);
        search.addActionListener(event -> reloadCatalogue());
        searchField.addActionListener(event -> reloadCatalogue());
        RoundButton clear = new RoundButton("Clear", RoundButton.Style.GHOST);
        clear.addActionListener(event -> {
            searchField.setText("");
            reloadCatalogue();
        });

        JPanel tools = new JPanel(new BorderLayout(10, 0));
        tools.setOpaque(false);
        tools.add(searchField, BorderLayout.CENTER);
        tools.add(row(search, clear), BorderLayout.EAST);

        JScrollPane scroll = tableScroll(catalogueTable);
        // Reaching the bottom of the list is the signal to fetch the next page.
        scroll.getVerticalScrollBar().addAdjustmentListener(event -> {
            JScrollBar bar = scroll.getVerticalScrollBar();
            boolean nearBottom = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 40;
            if (nearBottom && !event.getValueIsAdjusting()) {
                loadNextCataloguePage();
            }
        });

        catalogueStatus.setFont(new Font(FAMILY, Font.PLAIN, 12));
        catalogueStatus.setForeground(MUTED);

        JPanel middle = new JPanel(new BorderLayout(0, 14));
        middle.setOpaque(false);
        middle.add(tools, BorderLayout.NORTH);
        middle.add(scroll, BorderLayout.CENTER);
        middle.add(catalogueStatus, BorderLayout.SOUTH);
        view.add(middle, BorderLayout.CENTER);

        RoundButton borrow = new RoundButton("Borrow Selected", RoundButton.Style.ACCENT);
        borrow.addActionListener(event -> withSelection(catalogueTable, catalogueModel,
                book -> report(process.borrow(book.getId()))));
        RoundButton giveBack = new RoundButton("Return Selected", RoundButton.Style.PRIMARY);
        giveBack.addActionListener(event -> withSelection(catalogueTable, catalogueModel,
                book -> report(process.returnBook(book.getId()))));
        removeButton.addActionListener(event -> withSelection(catalogueTable, catalogueModel,
                this::confirmRemoval));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(row(borrow, giveBack, removeButton), BorderLayout.WEST);
        view.add(footer, BorderLayout.SOUTH);
        return view;
    }

    private JComponent buildMyBooksView() {
        Panel view = new Panel();
        view.setLayout(new BorderLayout(0, 14));
        view.add(sectionTitle("My Books", "Everything you have on loan right now."),
                BorderLayout.NORTH);
        view.add(tableScroll(myBooksTable), BorderLayout.CENTER);

        RoundButton giveBack = new RoundButton("Return Selected", RoundButton.Style.PRIMARY);
        giveBack.addActionListener(event -> withSelection(myBooksTable, myBooksModel,
                book -> report(process.returnBook(book.getId()))));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(row(giveBack), BorderLayout.WEST);
        view.add(footer, BorderLayout.SOUTH);
        return view;
    }

    private JComponent buildAddBookView() {
        Panel view = new Panel();
        view.setLayout(new BorderLayout(0, 14));
        view.add(sectionTitle("Add a Book", "Register a new title in the catalogue."),
                BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(460, 0));

        form.add(field("Title", bookTitle));
        form.add(field("Author", bookAuthor));
        bookCategory.setEditable(true);
        styleCombo(bookCategory);
        form.add(field("Category", bookCategory));
        form.add(label("Pick an existing category, or type a new one.",
                Font.PLAIN, 12, MUTED));

        RoundButton add = new RoundButton("Add to Catalogue", RoundButton.Style.PRIMARY);
        add.addActionListener(event -> doAddBook());
        RoundButton reset = new RoundButton("Clear Form", RoundButton.Style.GHOST);
        reset.addActionListener(event -> clearBookForm());
        form.add(Box.createVerticalStrut(16));
        form.add(row(add, reset));
        form.add(Box.createVerticalGlue());

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(form, BorderLayout.WEST);
        view.add(holder, BorderLayout.CENTER);
        return view;
    }

    /* ===================================================================== */
    /* ACTIONS - every one of them delegates straight to the logic layer     */
    /* ===================================================================== */

    private void doSignIn() {
        String username = loginUsername.getText();
        runAsync(loginMessage, "Signing in...",
                () -> process.signIn(username, loginPassword.getPassword0()),
                result -> {
                    if (!result.isSuccess()) {
                        loginMessage.showError(result.getMessage());
                        offerVerificationIfUnconfirmed(result.getMessage(), username);
                        return;
                    }
                    loginMessage.hideMessage();
                    loginPassword.setText("");
                    enterApplication(result.getMessage());
                });
    }

    /**
     * An account that exists but has never been confirmed is a dead end unless the
     * member is offered a way back into the code screen, so watch for that one case.
     */
    private void offerVerificationIfUnconfirmed(String message, String username) {
        if (!message.toLowerCase(Locale.ROOT).contains("confirm your email")) {
            return;
        }
        pendingUsername = username.trim();
        pendingPurpose = CodePurpose.SIGN_UP;
        int answer = JOptionPane.showConfirmDialog(this,
                "This account has not been confirmed yet.\nEnter the code we emailed you?",
                "Confirm your email", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            openVerificationStep("Check your email",
                    "Enter the code we sent you to finish setting up @" + pendingUsername + ".");
        }
    }

    private void doSignUp() {
        String username = signUpUsername.getText();
        runAsync(signUpMessage, "Creating your account and sending the code...",
                () -> process.signUp(username, signUpName.getText(), signUpEmail.getText(),
                        signUpPassword.getPassword0(), signUpConfirm.getPassword0()),
                result -> {
                    if (!result.isSuccess()) {
                        signUpMessage.showError(result.getMessage());
                        return;
                    }
                    User created = result.getValue().orElseThrow();
                    pendingUsername = created.getUsername();
                    pendingPurpose = CodePurpose.SIGN_UP;
                    clearSignUpForm();
                    openVerificationStep("Check your email",
                            "We sent a code to " + created.getMaskedEmail()
                                    + ". Enter it below to finish signing up.");
                    verifyMessage.showSuccess(result.getMessage());
                },
                signUpSubmit);
    }

    private void doVerify() {
        runAsync(verifyMessage, "Checking your code...",
                () -> process.verifyAccount(pendingUsername, verifyCode.getText()),
                result -> {
                    if (!result.isSuccess()) {
                        verifyMessage.showError(result.getMessage());
                        verifyCode.selectAll();
                        return;
                    }
                    String username = pendingUsername;
                    clearVerifyForm();
                    openForm(FORM_LOGIN);
                    loginUsername.setText(username);
                    loginMessage.showSuccess(result.getMessage());
                    loginPassword.requestFocusInWindow();
                },
                verifySubmit);
    }

    private void doRequestReset() {
        String username = forgotUsername.getText();
        runAsync(forgotMessage, "Sending your code...",
                () -> process.requestPasswordReset(username),
                result -> {
                    if (!result.isSuccess()) {
                        forgotMessage.showError(result.getMessage());
                        return;
                    }
                    pendingUsername = username.trim();
                    pendingPurpose = CodePurpose.PASSWORD_RESET;
                    forgotUsername.setText("");
                    forgotMessage.hideMessage();
                    setWrappedText(resetBlurb,
                            "Sent to " + result.getValue().orElse("your email") + ".", 286);
                    openForm(FORM_RESET);
                    resetMessage.showSuccess(result.getMessage());
                    resetCode.requestFocusInWindow();
                },
                forgotSend);
    }

    private void doResetPassword() {
        runAsync(resetMessage, "Updating your password...",
                () -> process.resetPassword(pendingUsername, resetCode.getText(),
                        resetPassword.getPassword0(), resetConfirm.getPassword0()),
                result -> {
                    if (!result.isSuccess()) {
                        resetMessage.showError(result.getMessage());
                        return;
                    }
                    String username = pendingUsername;
                    clearResetForm();
                    openForm(FORM_LOGIN);
                    loginUsername.setText(username);
                    loginMessage.showSuccess(result.getMessage());
                    loginPassword.requestFocusInWindow();
                },
                resetSubmit);
    }

    /** One resend handler serves both code screens. */
    private void doResend(MessageBar bar, JButton trigger) {
        runAsync(bar, "Sending another code...",
                () -> process.resendCode(pendingUsername, pendingPurpose),
                result -> {
                    if (result.isSuccess()) {
                        bar.showSuccess(result.getMessage());
                    } else {
                        bar.showError(result.getMessage());
                    }
                },
                trigger);
    }

    private void openVerificationStep(String heading, String blurb) {
        verifyHeading.setText(heading);
        setWrappedText(verifyBlurb, blurb, 286);
        verifyCode.setText("");
        openForm(FORM_VERIFY);
        verifyCode.requestFocusInWindow();
    }

    private void doSignOut() {
        process.signOut();
        appMessage.hideMessage();
        searchField.setText("");
        openForm(FORM_LOGIN);
        show(screens, SCREEN_AUTH);
        loginMessage.showInfo("You have been signed out.");
        loginUsername.requestFocusInWindow();
    }

    private void doAddBook() {
        Result<Book> result = process.addBook(bookTitle.getText(),
                bookAuthor.getText(), String.valueOf(bookCategory.getEditor().getItem()));
        if (result.isSuccess()) {
            clearBookForm();
        }
        report(result);
    }

    private void confirmRemoval(Book book) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Remove \"" + book.getTitle() + "\" from the catalogue?",
                "Confirm removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            report(process.removeBook(book.getId()));
        }
    }

    /**
     * Shows the outcome of any operation. Rather than reloading every screen, the
     * others are simply marked stale and will reload when they are next opened.
     */
    private void report(Result<Book> result) {
        if (result.isSuccess()) {
            appMessage.showSuccess(result.getMessage());
        } else {
            appMessage.showError(result.getMessage());
        }
        markEverythingStale();
    }

    private void withSelection(JTable table, BookTableModel model, Consumer<Book> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            appMessage.showInfo("Select a book from the table first.");
            return;
        }
        action.accept(model.bookAt(table.convertRowIndexToModel(row)));
    }

    /* ===================================================================== */
    /* NAVIGATION + REFRESH                                                  */
    /* ===================================================================== */

    private void enterApplication(String welcome) {
        Optional<User> user = process.getCurrentUser();
        accountName.setText(user.map(User::getFullName).orElse(""));
        accountRole.setText(user.map(account -> account.getRole().getLabel()
                + "  ·  @" + account.getUsername()).orElse(""));
        avatar.setInitial(user.map(User::getInitial).orElse("?"));
        greeting.setText("Hello, " + user.map(account ->
                account.getFullName().split(" ")[0]).orElse("reader") + "!");
        removeButton.setVisible(user.map(User::isLibrarian).orElse(false));

        markEverythingStale();
        openView(VIEW_DASHBOARD);
        show(screens, SCREEN_APP);
        appMessage.showSuccess(welcome);
    }

    /**
     * Shows a view, building it the first time it is asked for and loading its data
     * only when something has changed since it was last looked at. Opening the
     * dashboard therefore never touches the catalogue, and vice versa.
     */
    private void openView(String name) {
        if (!builtViews.contains(name)) {
            views.add(viewBuilders.get(name).get(), name);
            builtViews.add(name);
            applyDensity();
        }
        currentView = name;
        show(views, name);
        for (NavButton button : navButtons) {
            button.setSelected(button.name().equals(name));
        }
        if (staleViews.remove(name)) {
            loadViewData(name);
        }
        if (VIEW_ADD_BOOK.equals(name)) {
            bookTitle.requestFocusInWindow();
        }
    }

    /* ===================================================================== */
    /* RESPONSIVE LAYOUT                                                     */
    /* ===================================================================== */

    /**
     * Re-flows the window for its current width. Three sizes are handled: the full
     * layout, a compact one that narrows the sidebar and stacks the dashboard tiles
     * two-by-two, and a narrow one that drops the sign-in artwork so the form keeps
     * the whole window.
     */
    private void applyResponsiveLayout() {
        int width = getWidth();
        boolean nowCompact = width < Constants.COMPACT_WIDTH;
        boolean nowNarrow = width < Constants.NARROW_WIDTH;

        brandPanel.setVisible(!nowNarrow);
        authForms.setBorder(new EmptyBorder(nowNarrow ? 16 : 28, nowNarrow ? 16 : 48,
                nowNarrow ? 16 : 28, nowNarrow ? 16 : 48));

        if (nowCompact != compact) {
            compact = nowCompact;
            sidebar.setPreferredSize(new Dimension(compact ? 76 : 248, 0));
            for (NavButton button : navButtons) {
                button.setCompact(compact);
            }
            sidebarCaption.setVisible(!compact);
            sidebarHint.setVisible(!compact);
            sidebarWordmark.setVisible(!compact);
            // The dashboard is built lazily, so its tiles may not exist yet. When
            // they are built later they read the flag and lay themselves out to match.
            if (tiles != null) {
                tiles.setLayout(new GridLayout(compact ? 2 : 1, compact ? 2 : 4, 14, 14));
                for (StatTile tile : List.of(tileTotal, tileAvailable, tileBorrowed, tileMine)) {
                    tile.setCompact(compact);
                }
            }
            applyDensity();
            revalidate();
        }
        repaint();
    }

    /** Trims the padding around the views when space is tight. */
    private void applyDensity() {
        int pad = compact ? 12 : 24;
        views.setBorder(new EmptyBorder(compact ? 12 : 20, pad, pad, pad));
    }

    /** Every view will reload the next time it is opened. */
    private void markEverythingStale() {
        staleViews.addAll(viewBuilders.keySet());
        staleViews.remove(currentView);
        loadViewData(currentView);
    }

    /** Fetches exactly what the named view needs, off the Swing thread. */
    private void loadViewData(String name) {
        switch (name) {
            case VIEW_CATALOGUE -> reloadCatalogue();
            case VIEW_MY_BOOKS -> loadInBackground(process::getMyBooks, myBooksModel::setBooks);
            case VIEW_ADD_BOOK -> loadInBackground(process::getCategories, this::showCategories);
            default -> {
                loadInBackground(process::getMyBooks, dashboardModel::setBooks);
                loadInBackground(process::getStatistics, this::showStatistics);
            }
        }
    }

    /**
     * Reads from MySQL on a worker thread and hands the result back on the Swing
     * thread. Keeping reads off the event thread is what stops the window greying
     * out while a query runs, and it is also where a dropped connection is reported.
     */
    private <T> void loadInBackground(Supplier<T> work, Consumer<T> whenDone) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                try {
                    whenDone.accept(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failed) {
                    Throwable cause = failed.getCause() == null ? failed : failed.getCause();
                    appMessage.showError(cause.getMessage());
                }
            }
        }.execute();
    }

    private void openForm(String name) {
        show(authForms, name);
        if (FORM_LOGIN.equals(name)) {
            loginUsername.requestFocusInWindow();
        } else {
            loginMessage.hideMessage();
        }
    }

    /** Starts the catalogue again from the first page, using the current search box. */
    private void reloadCatalogue() {
        catalogueQuery = searchField.getText();
        catalogueLoaded = 0;
        catalogueTotal = 0;
        catalogueModel.setBooks(List.of());
        loadNextCataloguePage();
    }

    /**
     * Fetches the next slice of rows and appends it. This is the lazy loading the
     * table is built on: nothing beyond the first {@value app.constants.Constants#PAGE_SIZE}
     * rows is read from MySQL until the reader scrolls far enough to need them.
     */
    private void loadNextCataloguePage() {
        if (catalogueLoading) {
            return;
        }
        if (catalogueLoaded > 0 && catalogueLoaded >= catalogueTotal) {
            return;
        }
        catalogueLoading = true;
        int offset = catalogueLoaded;
        String query = catalogueQuery;
        catalogueStatus.setText("Loading...");
        loadInBackground(() -> process.getCatalogPage(query, offset), page -> {
            catalogueLoading = false;
            if (!query.equals(catalogueQuery)) {
                return;      // the search box moved on while this was in flight
            }
            catalogueModel.addBooks(page.getItems());
            catalogueLoaded = page.getLoadedCount();
            catalogueTotal = page.getTotal();
            showCatalogueCount();
        });
    }

    private void showCatalogueCount() {
        if (catalogueTotal == 0) {
            catalogueStatus.setText("No books match that search.");
        } else if (catalogueLoaded >= catalogueTotal) {
            catalogueStatus.setText("Showing all " + catalogueTotal + " books.");
        } else {
            catalogueStatus.setText("Showing " + catalogueLoaded + " of " + catalogueTotal
                    + "  ·  scroll for more");
        }
    }

    private void showStatistics(Statistics statistics) {
        tileTotal.setValue(statistics.getTotal());
        tileAvailable.setValue(statistics.getAvailable());
        tileBorrowed.setValue(statistics.getBorrowed());
        tileMine.setValue(statistics.getMine());
        tileBorrowed.setCaption(statistics.getOverdue() + " overdue");
    }

    private void showCategories(List<String> categories) {
        Object typed = bookCategory.getEditor().getItem();
        bookCategory.removeAllItems();
        for (String category : categories) {
            bookCategory.addItem(category);
        }
        bookCategory.getEditor().setItem(typed == null ? "" : typed);
    }

    /**
     * Runs work that talks to MySQL or an SMTP server on a background thread, so the
     * window keeps repainting while an email is being delivered. The buttons passed in
     * are disabled for the duration, which also stops a double click sending twice.
     */
    private <T> void runAsync(MessageBar bar, String busyMessage,
            Supplier<Result<T>> work, Consumer<Result<T>> whenDone,
            JComponent... toDisable) {
        for (JComponent component : toDisable) {
            component.setEnabled(false);
        }
        bar.showInfo(busyMessage);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Result<T>, Void>() {
            @Override
            protected Result<T> doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                for (JComponent component : toDisable) {
                    component.setEnabled(true);
                }
                try {
                    whenDone.accept(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failed) {
                    Throwable cause = failed.getCause() == null ? failed : failed.getCause();
                    bar.showError("Something went wrong: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void clearSignUpForm() {
        for (JTextField input : List.of(signUpName, signUpUsername, signUpEmail,
                signUpPassword, signUpConfirm)) {
            input.setText("");
        }
        signUpMessage.hideMessage();
    }

    private void clearVerifyForm() {
        verifyCode.setText("");
        verifyMessage.hideMessage();
    }

    private void clearResetForm() {
        for (JTextField input : List.of(resetCode, resetPassword, resetConfirm)) {
            input.setText("");
        }
        forgotUsername.setText("");
        resetMessage.hideMessage();
        forgotMessage.hideMessage();
    }

    private void clearBookForm() {
        bookTitle.setText("");
        bookAuthor.setText("");
        bookCategory.getEditor().setItem("");
    }

    private static void show(JPanel host, String name) {
        ((CardStack) host.getLayout()).show(host, name);
    }

    /* ===================================================================== */
    /* SHARED WIDGET FACTORIES - the DRY part of the interface               */
    /* ===================================================================== */

    private static JLabel label(String text, int style, int size, Color colour) {
        String html = text;
        if (!text.startsWith("<html>") && text.contains("\n")) {
            html = "<html>" + text.replace("\n", "<br>") + "</html>";
        }
        JLabel label = new JLabel(html);
        label.setFont(new Font(FAMILY, style, size));
        label.setForeground(colour);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Without a ceiling a BoxLayout stretches labels, which opens odd gaps
        // vertically and pushes the widget beside them out to the far edge.
        label.setMaximumSize(label.getPreferredSize());
        return label;
    }

    /** A captioned input row, used by every form field in the application. */
    private static JComponent field(String caption, JComponent input) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel label = label(caption.toUpperCase(Locale.ROOT), Font.BOLD, 10, MUTED);
        group.add(label);
        group.add(Box.createVerticalStrut(6));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                Math.max(42, input.getPreferredSize().height)));
        group.add(input);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                group.getPreferredSize().height));
        return group;
    }

    /** Places two captioned fields side by side. */
    private static JComponent pair(JComponent left, JComponent right) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 14, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(left);
        panel.add(right);
        return panel;
    }

    /** Lays widgets out left to right at their natural size. */
    private static JPanel row(JComponent... children) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int index = 0; index < children.length; index++) {
            if (index > 0) {
                panel.add(Box.createHorizontalStrut(10));
            }
            panel.add(children[index]);
        }
        panel.add(Box.createHorizontalGlue());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                panel.getPreferredSize().height + 2));
        return panel;
    }

    private static JComponent stretch(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        component.setPreferredSize(new Dimension(component.getPreferredSize().width, 46));
        return component;
    }

    private static JComponent divider() {
        JPanel line = new JPanel();
        line.setBackground(LINE);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(10, 1));
        return line;
    }

    private static JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(FAMILY, Font.BOLD, 13));
        button.setForeground(BLUE);
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    private static JComponent sectionTitle(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(label(title, Font.BOLD, 19, INK));
        panel.add(Box.createVerticalStrut(3));
        panel.add(label(subtitle, Font.PLAIN, 12, MUTED));
        return panel;
    }

    /** Replaces the heavy native drop-down with one that matches the rest of the theme. */
    private static void styleCombo(JComboBox<String> combo) {
        combo.setUI(new FlatComboUI());
        combo.setFont(new Font(FAMILY, Font.PLAIN, 14));
        combo.setBackground(WHITE);
        combo.setForeground(INK);
        combo.setFocusable(false);
        combo.setPreferredSize(new Dimension(220, 42));
        combo.setBorder(new RoundedBorder(LINE, 10, 4));
        combo.setRenderer(new OptionRenderer());

        Component editor = combo.getEditor().getEditorComponent();
        if (editor instanceof JTextField input) {
            input.setFont(new Font(FAMILY, Font.PLAIN, 14));
            input.setForeground(INK);
            input.setBackground(WHITE);
            input.setCaretColor(BLUE);
            input.setBorder(new EmptyBorder(0, 6, 0, 0));
        }
    }

    /** Makes a scroll bar a thin rounded track with no arrow buttons. */
    private static void styleScrollBar(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(11, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 11));
    }

    /** Centres a form card in the available space and scrolls it when the window is short. */
    private static JComponent wrapScrollable(JComponent content) {
        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints placement = new GridBagConstraints();
        placement.gridx = 0;
        placement.gridy = 0;
        placement.weightx = 1;
        placement.weighty = 1;
        placement.anchor = GridBagConstraints.CENTER;
        placement.fill = GridBagConstraints.NONE;
        centre.add(content, placement);

        JScrollPane scroll = new JScrollPane(centre);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollBar(scroll);
        return scroll;
    }

    private static JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new RoundedBorder(LINE, 10, 0));
        scroll.getViewport().setBackground(WHITE);
        styleScrollBar(scroll);
        return scroll;
    }

    private static JTable styledTable(BookTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font(FAMILY, Font.PLAIN, 13));
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setBackground(WHITE);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        BookCellRenderer renderer = new BookCellRenderer();
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            tableColumn.setCellRenderer(renderer);
            tableColumn.setPreferredWidth(model.widthOf(column));
        }
        return table;
    }

    /* ===================================================================== */
    /* REUSABLE COMPONENTS                                                   */
    /* ===================================================================== */

    /** CardLayout with a name that reads better at the call sites. */
    private static final class CardStack extends java.awt.CardLayout {
        private CardStack() {
            super(0, 0);
        }
    }

    /** A flat white rounded surface used for every content block. */
    private static class Panel extends JPanel {

        private Panel() {
            setOpaque(false);
            setBorder(new EmptyBorder(20, 22, 20, 22));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            canvas.setColor(WHITE);
            canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            canvas.setColor(LINE);
            canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            canvas.dispose();
            super.paintComponent(graphics);
        }
    }

    /** A white card that stacks its children vertically - the form container. */
    private static final class Card extends Panel {

        private static final int CARD_WIDTH = 430;

        private Card() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(30, 32, 26, 32));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(CARD_WIDTH, super.getPreferredSize().height);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(CARD_WIDTH, super.getMaximumSize().height);
        }
    }

    /** Vertical gradient background, used by the brand column and the sidebar. */
    private static final class GradientPanel extends JPanel {

        private final Color top;
        private final Color bottom;

        private GradientPanel(Color top, Color bottom) {
            this.top = top;
            this.bottom = bottom;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            canvas.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            canvas.fillRect(0, 0, getWidth(), getHeight());
            canvas.setColor(YELLOW);
            canvas.fillRect(0, getHeight() - 4, getWidth(), 4);
            canvas.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Pill-shaped button in four theme variants. */
    private static final class RoundButton extends JButton {

        private enum Style {
            PRIMARY, ACCENT, GHOST, DANGER
        }

        private final Style style;
        private boolean hovered;

        private RoundButton(String text, Style style) {
            super(text);
            this.style = style;
            setFont(new Font(FAMILY, Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(11, 20, 11, 20));
            setForeground(foreground());
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        private Color foreground() {
            return switch (style) {
                case PRIMARY, DANGER -> WHITE;
                case ACCENT -> BLUE_DEEP;
                case GHOST -> BLUE;
            };
        }

        private Color background() {
            return switch (style) {
                case PRIMARY -> hovered ? BLUE_HOVER : BLUE;
                case ACCENT -> hovered ? YELLOW_HOVER : YELLOW;
                case DANGER -> hovered ? DANGER.darker() : DANGER;
                case GHOST -> hovered ? BLUE_SOFT : WHITE;
            };
        }

        /** Colour is decided here, never inside paint, so no repaint loop can start. */
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (style != null) {
                setForeground(enabled ? foreground() : MUTED);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            int arc = Math.min(getHeight(), 24);
            if (!isEnabled()) {
                canvas.setColor(LINE);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                canvas.dispose();
                super.paintComponent(graphics);
                return;
            }
            canvas.setColor(background());
            canvas.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            if (style == Style.GHOST) {
                canvas.setColor(BLUE);
                canvas.setStroke(new BasicStroke(1.4f));
                canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            canvas.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Sidebar navigation entry; the active one is highlighted in yellow. */
    private static final class NavButton extends JButton {

        private final String fullText;
        private boolean active;
        private boolean hovered;

        /** In the compact sidebar only the initial is shown, with the name on hover. */
        private void setCompact(boolean compact) {
            setText(compact ? fullText.substring(0, 1) : fullText);
            setToolTipText(compact ? fullText : null);
            setHorizontalAlignment(compact ? SwingConstants.CENTER : SwingConstants.LEFT);
            setBorder(new EmptyBorder(12, compact ? 0 : 16, 12, compact ? 0 : 12));
        }

        /** The label the button was created with, whatever is drawn right now. */
        private String name() {
            return fullText;
        }

        private NavButton(String text) {
            super(text);
            this.fullText = text;
            setFont(new Font(FAMILY, Font.BOLD, 13));
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(WHITE);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(12, 16, 12, 12));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            this.active = selected;
            setForeground(selected ? BLUE_DEEP : WHITE);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            if (active) {
                canvas.setColor(YELLOW);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (hovered) {
                canvas.setColor(new Color(255, 255, 255, 38));
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            canvas.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Rounded text input that paints its own placeholder. */
    private static class HintField extends JTextField {

        private final String hint;

        private HintField(String hint, int columns) {
            super(columns);
            this.hint = hint;
            setFont(new Font(FAMILY, Font.PLAIN, 14));
            setForeground(INK);
            setBackground(WHITE);
            setCaretColor(BLUE);
            setBorder(new RoundedBorder(LINE, 10, 10));
            setPreferredSize(new Dimension(getPreferredSize().width, 42));
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setBackground(enabled ? WHITE : SURFACE);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty()) {
                paintHint(this, hint, graphics);
            }
        }
    }

    /** The password twin of {@link HintField}. */
    private static final class HintPasswordField extends JPasswordField {

        private final String hint;

        private HintPasswordField(String hint, int columns) {
            super(columns);
            this.hint = hint;
            setFont(new Font(FAMILY, Font.PLAIN, 14));
            setForeground(INK);
            setBackground(WHITE);
            setCaretColor(BLUE);
            setEchoChar('•');
            setBorder(new RoundedBorder(LINE, 10, 10));
            setPreferredSize(new Dimension(getPreferredSize().width, 42));
        }

        private String getPassword0() {
            return new String(getPassword());
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setBackground(enabled ? WHITE : SURFACE);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getPassword().length == 0) {
                paintHint(this, hint, graphics);
            }
        }
    }

    /**
     * Draws placeholder text inside an empty input. {@link HintField} and
     * {@link HintPasswordField} extend different Swing classes, so sharing this
     * method is how the two avoid duplicating the drawing code.
     */
    private static void paintHint(JTextField input, String hint, Graphics graphics) {
        Graphics2D canvas = prepare(graphics);
        canvas.setColor(new Color(0x9AA8BC));
        canvas.setFont(input.getFont().deriveFont(Font.PLAIN));
        Insets insets = input.getInsets();
        int baseline = input.getHeight() / 2 + canvas.getFontMetrics().getAscent() / 2 - 2;
        int left = insets.left + 2;
        if (input.getHorizontalAlignment() == SwingConstants.CENTER) {
            left = (input.getWidth() - canvas.getFontMetrics().stringWidth(hint)) / 2;
        }
        canvas.drawString(hint, left, baseline);
        canvas.dispose();
    }

    /** Sets label text that wraps at a width instead of being cut off at the card edge. */
    private static void setWrappedText(JLabel label, String text, int width) {
        label.setText("<html><div style='width:" + width + "px'>" + text + "</div></html>");
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
        label.revalidate();
    }

    /** Rounded one-pixel border with inner padding. */
    private static final class RoundedBorder implements Border {

        private final Color colour;
        private final int arc;
        private final int padding;

        private RoundedBorder(Color colour, int arc, int padding) {
            this.colour = colour;
            this.arc = arc;
            this.padding = padding;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y,
                int width, int height) {
            Graphics2D canvas = prepare(graphics);
            canvas.setColor(component.isEnabled() ? colour : LINE);
            canvas.setStroke(new BasicStroke(1f));
            canvas.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            canvas.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(padding, padding + 8, padding, padding + 8);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** Flat drop-down: no bevel, a drawn chevron instead of the native arrow. */
    private static final class FlatComboUI extends BasicComboBoxUI {

        /**
         * Swing fills this area from the look-and-feel's {@code ComboBox.background}
         * default rather than from the component, which is why setting the combo's
         * background alone leaves a grey box behind the selected value.
         */
        @Override
        public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds,
                boolean hasFocus) {
            graphics.setColor(comboBox.isEnabled() ? WHITE : SURFACE);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D canvas = prepare(graphics);
                    canvas.setColor(BLUE);
                    canvas.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND));
                    int centreX = getWidth() / 2;
                    int centreY = getHeight() / 2;
                    canvas.drawLine(centreX - 4, centreY - 2, centreX, centreY + 2);
                    canvas.drawLine(centreX, centreY + 2, centreX + 4, centreY - 2);
                    canvas.dispose();
                }
            };
            arrow.setPreferredSize(new Dimension(28, 28));
            arrow.setContentAreaFilled(false);
            arrow.setBorderPainted(false);
            arrow.setFocusPainted(false);
            arrow.setOpaque(false);
            arrow.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return arrow;
        }
    }

    /** Drop-down list entries with padding and a soft blue highlight. */
    private static final class OptionRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            setFont(new Font(FAMILY, Font.PLAIN, 13));
            setBorder(new EmptyBorder(8, 12, 8, 12));
            setBackground(selected ? BLUE_SOFT : WHITE);
            setForeground(INK);
            return this;
        }
    }

    /** Thin rounded scroll bar without arrow buttons. */
    private static final class SlimScrollBarUI extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            trackColor = SURFACE;
            thumbColor = new Color(0xB9C8DD);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return hiddenButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return hiddenButton();
        }

        private JButton hiddenButton() {
            JButton button = new JButton();
            Dimension none = new Dimension(0, 0);
            button.setPreferredSize(none);
            button.setMinimumSize(none);
            button.setMaximumSize(none);
            return button;
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
            graphics.setColor(SURFACE);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            if (bounds.isEmpty()) {
                return;
            }
            Graphics2D canvas = prepare(graphics);
            canvas.setColor(isDragging ? BLUE : new Color(0xB9C8DD));
            canvas.fillRoundRect(bounds.x + 2, bounds.y + 2,
                    bounds.width - 4, bounds.height - 4, 8, 8);
            canvas.dispose();
        }
    }

    /** Inline feedback strip; colour follows the kind of message. */
    private static final class MessageBar extends JPanel {

        /** Space reserved below the strip so it never touches the next widget. */
        private static final int GAP = 12;

        private final JLabel text = new JLabel();
        private final int wrapWidth;
        private Color accent = BLUE;

        private MessageBar(int wrapWidth) {
            this.wrapWidth = wrapWidth;
            setLayout(new BorderLayout());
            setOpaque(false);
            setVisible(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(new EmptyBorder(11, 14, 11 + GAP, 14));
            text.setFont(new Font(FAMILY, Font.PLAIN, 13));
            add(text, BorderLayout.CENTER);
        }

        private void showSuccess(String message) {
            render(message, SUCCESS);
        }

        private void showError(String message) {
            render(message, DANGER);
        }

        private void showInfo(String message) {
            render(message, BLUE);
        }

        private void hideMessage() {
            setVisible(false);
            revalidate();
        }

        /** A fixed wrap width keeps the reported height correct inside a BoxLayout. */
        private void render(String message, Color colour) {
            this.accent = colour;
            text.setText("<html><div style='width:" + wrapWidth + "px'>" + message + "</div></html>");
            text.setForeground(colour.darker());
            setVisible(true);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            int height = getHeight() - GAP;
            canvas.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 26));
            canvas.fillRoundRect(0, 0, getWidth() - 1, height - 1, 10, 10);
            canvas.setColor(accent);
            canvas.fillRoundRect(0, 0, 4, height - 1, 4, 4);
            canvas.dispose();
            super.paintComponent(graphics);
        }
    }

    /** One number on the dashboard. */
    private static final class StatTile extends JPanel {

        private final JLabel value = new JLabel("0");
        private final JLabel caption;
        private final Color accent;

        private StatTile(String title, Color accent) {
            this.accent = accent;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(18, 20, 18, 20));

            JLabel heading = label(title.toUpperCase(Locale.ROOT), Font.BOLD, 10, MUTED);
            value.setFont(new Font(FAMILY, Font.BOLD, 30));
            value.setForeground(accent);
            value.setAlignmentX(Component.LEFT_ALIGNMENT);
            caption = label("", Font.PLAIN, 11, MUTED);

            add(heading);
            add(Box.createVerticalStrut(8));
            add(value);
            add(Box.createVerticalStrut(2));
            add(caption);
        }

        private void setValue(int number) {
            value.setText(String.valueOf(number));
        }

        /** Trims the tile so four of them stacked two-by-two still leave room below. */
        private void setCompact(boolean compact) {
            setBorder(new EmptyBorder(compact ? 10 : 18, compact ? 14 : 20,
                    compact ? 10 : 18, compact ? 14 : 20));
            value.setFont(new Font(FAMILY, Font.BOLD, compact ? 24 : 30));
            value.setMaximumSize(null);
            revalidate();
        }

        /** The ceiling has to be recomputed, or the label keeps its empty height. */
        private void setCaption(String note) {
            caption.setText(note);
            caption.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    caption.getPreferredSize().height));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            canvas.setColor(WHITE);
            canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            canvas.setColor(LINE);
            canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            canvas.setColor(accent);
            canvas.fillRoundRect(0, 14, 4, 26, 4, 4);
            canvas.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Yellow square with a blue "D" - the application mark. */
    private static final class Monogram extends JComponent {

        private Monogram() {
            setPreferredSize(new Dimension(44, 44));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            canvas.setColor(YELLOW);
            canvas.fillRoundRect(0, 0, 44, 44, 14, 14);
            canvas.setColor(BLUE_DEEP);
            canvas.setFont(new Font(FAMILY, Font.BOLD, 24));
            canvas.drawString("D", 14, 31);
            canvas.dispose();
        }
    }

    /** Circular initial shown in the top bar. */
    private static final class Avatar extends JComponent {

        private String initial = "?";

        private Avatar() {
            setPreferredSize(new Dimension(40, 40));
        }

        private void setInitial(String initial) {
            this.initial = initial;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            canvas.setColor(BLUE);
            canvas.fillOval(0, 0, 40, 40);
            canvas.setColor(YELLOW);
            canvas.setStroke(new BasicStroke(2f));
            canvas.drawOval(1, 1, 37, 37);
            canvas.setColor(WHITE);
            canvas.setFont(new Font(FAMILY, Font.BOLD, 17));
            int width = canvas.getFontMetrics().stringWidth(initial);
            canvas.drawString(initial, (40 - width) / 2, 26);
            canvas.dispose();
        }
    }

    /** Small filled circle used as a list marker. */
    private static final class Dot extends JComponent {

        private final Color colour;

        private Dot(Color colour) {
            this.colour = colour;
            setPreferredSize(new Dimension(9, 9));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = prepare(graphics);
            canvas.setColor(colour);
            canvas.fillOval(0, getHeight() / 2 - 4, 8, 8);
            canvas.dispose();
        }
    }

    /* ===================================================================== */
    /* TABLE SUPPORT                                                         */
    /* ===================================================================== */

    /** Shows a list of books; one model class serves all three tables (DRY). */
    private static final class BookTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {"ID", "Title", "Author", "Category",
                "Status", "Borrower", "Due"};
        private static final int[] WIDTHS = {50, 260, 170, 150, 100, 120, 110};
        private static final int STATUS_COLUMN = 4;

        private List<Book> books = List.of();

        private void setBooks(List<Book> books) {
            this.books = books;
            fireTableDataChanged();
        }

        /** Appends the next page without disturbing the rows already on screen. */
        private void addBooks(List<Book> more) {
            if (more.isEmpty()) {
                return;
            }
            int firstNewRow = books.size();
            List<Book> combined = new ArrayList<>(books);
            combined.addAll(more);
            books = List.copyOf(combined);
            fireTableRowsInserted(firstNewRow, books.size() - 1);
        }

        private Book bookAt(int row) {
            return books.get(row);
        }

        private int widthOf(int column) {
            return WIDTHS[column];
        }

        @Override
        public int getRowCount() {
            return books.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            Book book = books.get(row);
            return switch (column) {
                case 0 -> book.getId();
                case 1 -> book.getTitle();
                case 2 -> book.getAuthor();
                case 3 -> book.getCategory();
                case 4 -> book.getStatus();
                case 5 -> book.getBorrowedBy().isEmpty() ? "-" : "@" + book.getBorrowedBy();
                default -> book.getDueDate().map(String::valueOf).orElse("-");
            };
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Integer.class : String.class;
        }
    }

    /** Blue header with white bold capitals. */
    private static final class HeaderRenderer implements TableCellRenderer {

        private final DefaultTableCellRenderer delegate = new DefaultTableCellRenderer();

        private HeaderRenderer() {
            delegate.setOpaque(true);
            delegate.setBackground(BLUE);
            delegate.setForeground(WHITE);
            delegate.setFont(new Font(FAMILY, Font.BOLD, 11));
            delegate.setBorder(new EmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            delegate.setText(String.valueOf(value).toUpperCase(Locale.ROOT));
            return delegate;
        }
    }

    /** Zebra striping, blue selection and a coloured status word. */
    private static final class BookCellRenderer extends DefaultTableCellRenderer {

        private BookCellRenderer() {
            setBorder(new EmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setFont(new Font(FAMILY, column == 1 ? Font.BOLD : Font.PLAIN, 13));
            setBackground(selected ? BLUE_SOFT : (row % 2 == 0 ? WHITE : SURFACE));
            setForeground(INK);

            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn == BookTableModel.STATUS_COLUMN) {
                String status = String.valueOf(value);
                setFont(new Font(FAMILY, Font.BOLD, 12));
                setForeground(switch (status) {
                    case "Available" -> SUCCESS;
                    case "Overdue" -> DANGER;
                    default -> new Color(0x8A6100);
                });
            }
            return this;
        }
    }

    /** Antialiased drawing surface - requested by every custom component. */
    private static Graphics2D prepare(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return canvas;
    }
}
