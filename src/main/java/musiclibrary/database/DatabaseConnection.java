package musiclibrary.database;

import com.zaxxer.hikari.HikariDataSource;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.*;

public class DatabaseConnection {
    private static DatabaseConnection INSTANCE;

    public static DatabaseConnection getINSTANCE() {
        if(INSTANCE == null) INSTANCE = new DatabaseConnection();
        return INSTANCE;
    }

    private HikariDataSource dataSource = null;
    private String[][] songsTable = null;
    private Map<String, Integer> artistsSongsMap = null;
    private Map<String, Integer> genreSongsMap = null;

    public String[][] getSongsTable() {
        return songsTable;
    }

    private DatabaseConnection() {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:oracle:thin:@localhost:1521:xe");
        dataSource.setUsername("MusicLibrary");
        dataSource.setPassword("12345678");

        System.out.println("Database connected.");
    }

    public String[][] getAllSongs() {
        try {
            songsTable = new String[getSongCount()][5];
            Connection conn = dataSource.getConnection();
            CallableStatement stmt = conn.prepareCall("SELECT * FROM table(MusicPackage.get_songs)");
            ResultSet set = stmt.executeQuery();

            int n = 0;
            while(set.next()) {
                songsTable[n] = new String[5];
                songsTable[n][0] = set.getString(1);
                songsTable[n][1] = set.getString(2);
                songsTable[n][2] = set.getString(3);
                songsTable[n][3] = set.getString(4);
                songsTable[n][4] = set.getString(5);
                n++;
            }

            analyzeDatabase();
            return songsTable;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getSongCount() {
        try {
            Connection conn = dataSource.getConnection();

            CallableStatement stmt = conn.prepareCall("SELECT COUNT(*) FROM SONGS");

            ResultSet set = stmt.executeQuery();
            set.next();

            int count = set.getInt(1);
            conn.close();

            return count;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void analyzeDatabase() {
        artistsSongsMap = new LinkedHashMap<>();
        genreSongsMap = new LinkedHashMap<>();

        for(String[] item : songsTable) {
            for(String artist : item[2].split(", ")) {
                if(artistsSongsMap.containsKey(artist)) artistsSongsMap.put(artist, artistsSongsMap.get(artist) + 1);
                else artistsSongsMap.put(artist, 1);
            }

            if(item[4] == null) continue;

            for(String genre : item[4].split(", ")) {
                if(genreSongsMap.containsKey(genre)) genreSongsMap.put(genre, genreSongsMap.get(genre) + 1);
                else genreSongsMap.put(genre, 1);
            }
        }



        artistsSongsMap = getSortedList(artistsSongsMap);
        genreSongsMap = getSortedList(genreSongsMap);
    }

    public void addGenres(String artistId, String... genres) {
        try {
            Connection conn = dataSource.getConnection();
            for(String g : genres) {

                CallableStatement stmt = conn.prepareCall("{CALL MusicPackage.add_genre (?, ?)}");

                stmt.setString(1, g);
                stmt.setString(2, artistId);
                stmt.execute();
                stmt.close();
            }

            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addSong(String id, String name, ArtistSimplified[] artists, AlbumSimplified album) {
        try {
            Connection conn = dataSource.getConnection();

            CallableStatement stmt = conn.prepareCall("{CALL MusicPackage.add_album (?, ?, ?)}");

            stmt.setString(1, album.getId());
            stmt.setString(2, album.getName());
            stmt.setString(3, album.getArtists()[0].getId());
            stmt.execute();
            stmt.close();

            stmt = conn.prepareCall("{CALL MusicPackage.add_song (?, ?, ?)}");

            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, album.getId());
            stmt.execute();
            stmt.close();

            for(ArtistSimplified artist : artists) {
                stmt = conn.prepareCall("{CALL MusicPackage.add_artist (?, ?, ?)}");

                stmt.setString(1, artist.getId());
                stmt.setString(2, artist.getName());
                stmt.setString(3, id);
                stmt.execute();
                stmt.close();
            }


            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveToFile(String path, String filename) {
        try {
            Connection conn = dataSource.getConnection();
            CallableStatement stmt = conn.prepareCall("{CALL MusicPackage.save_file(?, ?)}");

            stmt.setString(1, path);
            stmt.setString(2, filename);
            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Integer> getSortedList(Map<String, Integer> map) {
        var sorted = new LinkedList<>(map.entrySet());
        sorted.sort(Map.Entry.comparingByValue());
        Collections.reverse(sorted);

        Map<String, Integer> newList = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            newList.put(entry.getKey(), entry.getValue());
        }
        return newList;
    }

    public Map<String, Integer> getArtistsSongsMap() {
        return artistsSongsMap;
    }

    public Map<String, Integer> getGenreSongsMap() {
        return genreSongsMap;
    }
}
