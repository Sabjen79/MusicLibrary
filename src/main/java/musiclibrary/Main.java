package musiclibrary;

import musiclibrary.database.DatabaseConnection;
import musiclibrary.spotify.SpotifyAuth;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.CallableStatement;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        DatabaseConnection.getINSTANCE();
        SpotifyAuth.sendLogin();

        while(!SpotifyAuth.connected) {
            Thread.sleep(1000);
        }

        SpotifyAuth.loadSongs();
    }
}
