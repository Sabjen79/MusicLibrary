package musiclibrary;

import musiclibrary.database.DatabaseConnection;
import musiclibrary.spotify.SpotifyAuth;
import musiclibrary.ui.ConnectionUI;
import musiclibrary.ui.MainUI;

import javax.swing.*;
import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.CallableStatement;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }

        ConnectionUI.show();
        //MainUI.show();
    }
}
