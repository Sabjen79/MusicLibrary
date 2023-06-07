package musiclibrary.ui;

import musiclibrary.database.DatabaseConnection;
import musiclibrary.database.FileSaver;
import musiclibrary.spotify.SpotifyAuth;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainUI {

    private static JFrame frame;
    private static JPanel topPanel, botPanel, centerPanel;
    private static JTable table;
    private static TableRowSorter<DatabaseTable> sorter;
    private static JLabel numLabel;
    private static JTextField filterName, filterArtist, filterAlbum, filterGenre;

    public static void show() {

        frame = new JFrame("MusicLibrary");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        panel.setLayout(new BorderLayout());

        centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        DatabaseTable model = new DatabaseTable();
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane pane = new JScrollPane(table);
        centerPanel.add(pane);

        topPanel = new JPanel();

        numLabel = new JLabel();
        numLabel.setText("Count: " + table.getRowCount());
        numLabel.setPreferredSize(new Dimension(100, 50));

        topPanel.add(numLabel);

        filterName = addField("Song Name:");
        filterArtist = addField("Artist Name:");
        filterAlbum = addField("Album Name:");
        filterGenre = addField("Genre Name:");

        botPanel = new JPanel();
        JButton reloadButton = new JButton("Reload");
        reloadButton.addActionListener((e) -> {
            int n = JOptionPane.showConfirmDialog(
                    frame,
                    "This will reload ALL of your spotify songs. Continue?",
                    "Alert",
                    JOptionPane.YES_NO_OPTION);

            if(n == 0) {
                MainUI.close();
                LoadSongsUI.show();
            }
        });
        botPanel.add(reloadButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener((e) -> {
            int option = JOptionPane.showOptionDialog(frame,
                    "How do you wish to save the database? (Filters can be used only for XML and JSON)",
                    "Save",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Full Plain Text", "Filtered XML", "Filtered JSON"},
                    null);

            final String[] types = new String[]{
                    ".txt", "*.txt,*.TXT",
                    ".xml", "*.xml,*.XML",
                    ".json", "*.json,*.JSON",
            };

            JFileChooser chooser = new JFileChooser();
            chooser.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }

                    return f.getName().toLowerCase().endsWith(types[option*2]);
                }

                @Override
                public String getDescription() {
                    return types[option*2+1];
                }
            });

            if(option == JOptionPane.CLOSED_OPTION) return;

            chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if(!path.endsWith(types[option*2])) path += types[option*2];
                FileSaver.saveFile(path, option, table);
            }
        });
        botPanel.add(saveButton);

        JButton statButton = new JButton("Statistics");
        statButton.addActionListener((e) -> {
            StatisticsUI.close();
            StatisticsUI.show();
        });

        botPanel.add(statButton);

        JButton imageButton = new JButton("Export Cover Art");
        imageButton.addActionListener((e) -> {
            if(table.getSelectedRow() == -1) return;

            String id = model.getTable()[table.convertRowIndexToModel(table.getSelectedRow())][0];
            BufferedImage img = SpotifyAuth.getCover(id);
            if(img == null) return;

            JFileChooser chooser = new JFileChooser();
            chooser.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }

                    return f.getName().toLowerCase().endsWith(".jpeg");
                }

                @Override
                public String getDescription() {
                    return "*.jpeg,*.JPEG";
                }
            });

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                FileSaver.saveImage(chooser.getSelectedFile().getAbsolutePath(), img);
            }

        });

        table.getSelectionModel().addListSelectionListener(event -> imageButton.setEnabled(table.getSelectedRow() != -1));
        imageButton.setEnabled(false);
        botPanel.add(imageButton);

        panel.add(BorderLayout.CENTER, centerPanel);
        panel.add(BorderLayout.NORTH, topPanel);
        panel.add(BorderLayout.SOUTH, botPanel);
        frame.getContentPane().add(panel);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);


    }

    public static void close() {
        frame.setVisible(false);
    }

    private static JTextField addField(String name) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));

        JLabel label = new JLabel(name, SwingConstants.CENTER);

        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(120, 20));
        field.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        update();
                    }
                    public void insertUpdate(DocumentEvent e) {
                        update();
                    }
                    public void removeUpdate(DocumentEvent e) {
                        update();
                    }
                });
        label.setLabelFor(field);
        panel.add(label, Component.TOP_ALIGNMENT);
        panel.add(field);

        topPanel.add(panel);

        return field;
    }

    private static void update() {
        ArrayList<RowFilter<Object,Object>> filters = new ArrayList<>();

        if(filterName.getText().length() > 0) filters.add(RowFilter.regexFilter("(?i)" + filterName.getText(), 0));
        if(filterArtist.getText().length() > 0) filters.add(RowFilter.regexFilter("(?i)" + filterArtist.getText(), 1));
        if(filterAlbum.getText().length() > 0) filters.add(RowFilter.regexFilter("(?i)" + filterAlbum.getText(), 2));
        if(filterGenre.getText().length() > 0) filters.add(RowFilter.regexFilter("(?i)" + filterGenre.getText(), 3));
        var rf = RowFilter.andFilter(filters);
        sorter.setRowFilter(rf);

        numLabel.setText("Count: " + table.getRowCount());
    }
}
