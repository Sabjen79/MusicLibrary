package musiclibrary.ui;

import musiclibrary.database.DatabaseConnection;
import musiclibrary.spotify.SpotifyAuth;

import javax.swing.*;
import javax.swing.border.Border;
import javax.xml.crypto.Data;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

public class StatisticsUI {
    private static JFrame frame;
    private static JPanel panel;

    public static void show() {
        frame = new JFrame("Statistics");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(550, 450);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new FlowLayout());

        addLabel("Your saved songs statistics...", frame.getWidth(), 30, true, 18);
        addLabel("Most listened artists:", frame.getWidth()/3, 20, false, 12);
        addLabel("Most listened genres:", frame.getWidth()/3, 20, false, 12);
        addTable(DatabaseConnection.getINSTANCE().getArtistsSongsMap(), new String[]{"Artist", "Songs"});
        addTable(DatabaseConnection.getINSTANCE().getGenreSongsMap(), new String[]{"Genre", "Songs"});

        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    public static void close() {
        if(frame != null) frame.setVisible(false);
    }

    private static void addLabel(String text, int width, int height, boolean bold, int fontSize) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(new Font("Serif", bold ? Font.BOLD : Font.PLAIN, fontSize));
        label.setVerticalAlignment(JLabel.CENTER);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setPreferredSize(new Dimension(width, height));

        panel.add(label);
    }

    private static void addTable(Map<String, Integer> map, String[] columnNames) {
        Object[][] data = new String[map.size()][2];

        int i = 0;
        for(var item : map.entrySet()) {
            data[i][0] = item.getKey();
            data[i++][1] = item.getValue() + " song" + ((item.getValue() != 1) ? "s" : "");
        }

        JTable table = new JTable(data, columnNames);
        table.setShowVerticalLines(false);

        JScrollPane pane = new JScrollPane(table);
        pane.setPreferredSize(new Dimension((int) (frame.getWidth()/2.5), 300));
        panel.add(pane);
    }
}
