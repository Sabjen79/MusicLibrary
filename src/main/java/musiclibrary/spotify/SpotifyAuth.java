package musiclibrary.spotify;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import musiclibrary.database.DatabaseConnection;
import musiclibrary.ui.ConnectionUI;
import musiclibrary.ui.LoadSongsUI;
import musiclibrary.ui.MainUI;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERequest;

import javax.imageio.ImageIO;
import javax.xml.crypto.Data;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import static se.michaelthelin.spotify.enums.AuthorizationScope.*;


public class SpotifyAuth {
    private static final String clientId = "396bb7aed0364999992bb080a32c360a";
    private static final URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:7999/spotify");
    private static String codeVerifier;
    public static boolean connected = false;
    private static Date availableUntil;

    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setRedirectUri(redirectUri)
            .build();

    public static SpotifyApi getApi() {
        if(availableUntil == null) refresh();

        if(new Date().after(availableUntil)) {
            refresh();
        }

        return spotifyApi;
    }

    public static int getSongsLength() {
        try {
            return getApi().getUsersSavedTracks().build().execute().getTotal();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadSongs() {
        (new Thread(() -> {
            try {
                int total = getApi().getUsersSavedTracks().build().execute().getTotal();
                ArrayList<String> artists = new ArrayList<>();

                for(int offset = 0; offset <= total; offset += 50) {
                    LoadSongsUI.setValue(offset, total + artists.size());

                    for (SavedTrack item : getApi().getUsersSavedTracks().offset(offset).limit(50).build().execute().getItems()) {
                        DatabaseConnection.getINSTANCE().addSong(
                                item.getTrack().getId(),
                                item.getTrack().getName(),
                                item.getTrack().getArtists(),
                                item.getTrack().getAlbum()
                        );

                        if(!artists.contains(item.getTrack().getArtists()[0].getId())) {
                            artists.add(item.getTrack().getArtists()[0].getId());
                        }
                    }
                }

                String[] strings = new String[50];
                for(int i = 0; i < artists.size(); i++) {
                    strings[i%50] = artists.get(i);

                    LoadSongsUI.setValue(total + i, total + artists.size());

                    if(i%50 == 49 || i == artists.size() - 1) {
                        var arts = getApi().getSeveralArtists(strings).build().execute();

                        for(var a : arts) {
                            DatabaseConnection.getINSTANCE().addGenres(a.getId(), a.getGenres());
                        }
                    }
                }

                LoadSongsUI.close();
                MainUI.show();
            } catch (IOException | ParseException | SpotifyWebApiException e) {
                throw new RuntimeException(e);
            }
        })).start();
    }

    public static void sendLogin() throws IOException {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int length = new Random().nextInt(43, 128);
        StringBuilder codeBuilder = new StringBuilder();
        for(int i = 0; i < length; i++) {
            codeBuilder.append(chars.charAt(new Random().nextInt(chars.length())));
        }

        codeVerifier = codeBuilder.toString();

        String codeChallenge = BaseEncoding.base64Url().omitPadding().encode(
                Hashing.sha256().hashString(codeVerifier, StandardCharsets.UTF_8).asBytes()
        );

        AuthorizationCodeUriRequest uriRequest = spotifyApi.authorizationCodePKCEUri(codeChallenge)
                .scope(PLAYLIST_READ_PRIVATE, USER_LIBRARY_READ, USER_READ_PRIVATE, USER_READ_RECENTLY_PLAYED)
                .build();

        URI uri = uriRequest.execute();

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        TemporaryServer.startServer();
    }

    public static BufferedImage getCover(String id) {
        BufferedImage returnValue = null;
        try {
            var images = getApi().getAlbum(getApi().getTrack(id).build().execute().getAlbum().getId()).build().execute().getImages();
            if(images == null || images.length == 0) return null;

            int max = -1;
            var image = images[0];
            for(var img : images) {
                if(max < img.getWidth()) {
                    max = img.getWidth();
                    image = img;
                }
            }

            returnValue = ImageIO.read(new URL(image.getUrl()));

        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    public static void auth(String code) {
        AuthorizationCodePKCERequest authorizationCodePKCERequest = spotifyApi.authorizationCodePKCE(code, codeVerifier)
                .build();

        try {
            final AuthorizationCodeCredentials credentials = authorizationCodePKCERequest.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            connected = true;
            availableUntil = new Date(System.currentTimeMillis() + credentials.getExpiresIn() * 1000L);

            ConnectionUI.close();
            LoadSongsUI.show();
        } catch (SpotifyWebApiException | ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void refresh() {
        AuthorizationCodePKCERefreshRequest request = spotifyApi.authorizationCodePKCERefresh().build();

        try {
            final AuthorizationCodeCredentials credentials = request.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            availableUntil = new Date(System.currentTimeMillis() + credentials.getExpiresIn() * 1000L);

        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
