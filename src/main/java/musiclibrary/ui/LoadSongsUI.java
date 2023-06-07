package musiclibrary.ui;

import musiclibrary.database.DatabaseConnection;
import musiclibrary.spotify.SpotifyAuth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class LoadSongsUI {
    private static JFrame frame;
    private static JPanel panel;
    private static JProgressBar progressBar;

    public static void show() {
        frame = new JFrame("Loading songs...");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BorderLayout());

        JLabel label = new JLabel("Loading your saved songs...", SwingConstants.CENTER);
        panel.add(BorderLayout.NORTH, label);

        progressBar = new JProgressBar(0, SpotifyAuth.getSongsLength());
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(50, 50));
        panel.add(BorderLayout.CENTER, progressBar);

        frame.getContentPane().add(panel);
        frame.setVisible(true);

        SpotifyAuth.loadSongs();
    }

    public static void setValue(int n, int max) {
        progressBar.setMaximum(max);
        progressBar.setValue(n);
        frame.repaint();
    }

    public static void close() {
        frame.setVisible(false);
    }
}

