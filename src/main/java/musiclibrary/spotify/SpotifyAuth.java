package musiclibrary.spotify;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import musiclibrary.database.DatabaseConnection;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERequest;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
        //if(availableUntil == null) refresh();

        if(new Date().after(availableUntil)) {
            refresh();
        }

        return spotifyApi;
    }

    public static void loadSongs() {
        try {
            int total = getApi().getUsersSavedTracks().build().execute().getTotal();

            for(int offset = 0; offset <= total; offset += 50) {
                System.out.println(offset + "/" + total);
                for (SavedTrack item : getApi().getUsersSavedTracks().offset(offset).limit(50).build().execute().getItems()) {
                    DatabaseConnection.getINSTANCE().addSong(
                            item.getTrack().getId(),
                            item.getTrack().getName(),
                            item.getTrack().getArtists(),
                            item.getTrack().getAlbum()
                    );
                }
            }





        } catch (IOException | ParseException | SpotifyWebApiException e) {
            throw new RuntimeException(e);
        }
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

    public static void auth(String code) {
        AuthorizationCodePKCERequest authorizationCodePKCERequest = spotifyApi.authorizationCodePKCE(code, codeVerifier)
                .build();

        try {
            final AuthorizationCodeCredentials credentials = authorizationCodePKCERequest.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            connected = true;
            availableUntil = new Date(System.currentTimeMillis() + credentials.getExpiresIn() * 1000L);
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
