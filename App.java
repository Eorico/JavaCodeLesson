import javax.swing.*;

public class App {

    public static void main(String[] args) {

        JFrame frame = new JFrame("Library System");

        JLabel label = new JLabel("Book Title:");

        JTextField textField = new JTextField();

        JButton button = new JButton("Search");

        label.setBounds(30, 30, 100, 30);
        textField.setBounds(120, 30, 200, 30);
        button.setBounds(120, 80, 100, 30);

        frame.add(label);
        frame.add(textField);
        frame.add(button);

        frame.setSize(400, 180);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        button.addActionListener(e -> {

            String bookTitle = textField.getText();

            label.setText("Searching: " + bookTitle);
        });
    }
}