package musiclibrary.database;

import com.zaxxer.hikari.HikariDataSource;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

import javax.xml.crypto.Data;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static DatabaseConnection INSTANCE;

    public static DatabaseConnection getINSTANCE() {
        if(INSTANCE == null) INSTANCE = new DatabaseConnection();
        return INSTANCE;
    }

    private HikariDataSource dataSource;

    private DatabaseConnection() {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:oracle:thin:@localhost:1521:xe");
        dataSource.setUsername("MusicLibrary");
        dataSource.setPassword("12345678");

        System.out.println("Database connected.");
    }

    public void addSong(String id, String name, ArtistSimplified[] artists, AlbumSimplified album) {
        try {
            Connection conn = dataSource.getConnection();

            CallableStatement stmt = conn.prepareCall("{CALL add_album (?, ?, ?)}");

            stmt.setString(1, album.getId());
            stmt.setString(2, album.getName());
            stmt.setString(3, album.getArtists()[0].getId());
            stmt.execute();
            stmt.close();

            stmt = conn.prepareCall("{CALL add_song (?, ?, ?)}");

            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, album.getId());
            stmt.execute();
            stmt.close();

            for(ArtistSimplified artist : artists) {
                stmt = conn.prepareCall("{CALL add_artist (?, ?, ?)}");

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

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
