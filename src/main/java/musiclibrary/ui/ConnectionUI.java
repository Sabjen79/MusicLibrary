package musiclibrary.ui;

import musiclibrary.database.DatabaseConnection;
import musiclibrary.spotify.SpotifyAuth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class ConnectionUI {
    private static JFrame frame;
    private static JPanel panel;
    private static JButton button;
    private static JProgressBar progressBar;

    public static void show() {
        frame = new JFrame("Connect to Spotify");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        panel = new JPanel();
        panel.setLayout(null);

        JLabel label = new JLabel("Connect with your spotify account.");
        label.setBounds(frame.getWidth()/2-80, frame.getHeight()/2-30-70, 200, 60);
        panel.add(label);


        button = new JButton("Redirect to browser...");
        button.setBounds(frame.getWidth()/2-100, frame.getHeight()/2-30, 200, 60);
        button.addActionListener((ActionEvent e) -> {
            try {
                DatabaseConnection.getINSTANCE();
                SpotifyAuth.sendLogin();

                button.setEnabled(false);
                button.setText("Waiting for authorization");
                frame.repaint();

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        panel.add(button);

        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    public static void close() {
        frame.setVisible(false);
    }
}
