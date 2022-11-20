import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.albums.GetAlbumsTracksRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import com.wrapper.spotify.requests.data.tracks.GetTrackRequest;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.concurrent.CompletableFuture;

public class SpotifyAPI {
    public SpotifyApi spotifyApi;

    public SpotifyAPI() {
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId("0d2694a4db6e4c45b4fa66d61cb249e5")
                .setClientSecret("6a4110de565a4b6b9f769aae95fca2d4")
                .build();
        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
                .build();
        try {
            ClientCredentials clientCredentials = clientCredentialsRequest.execute();

            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());
        } catch (Exception e) {
            // Something went wrong
            System.out.println("bad");
        }
    }

    public void refreshCredentials() {
        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
                .build();
        try {
            ClientCredentials clientCredentials = clientCredentialsRequest.execute();

            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());
        } catch (Exception e) {
            // Something went wrong
            System.out.println("bad");
        }
    }

    public void getTrack(String uri, YoutubeAudioManager youtube, MessageReceivedEvent event) {
        GetTrackRequest getTrackRequest = spotifyApi.getTrack(uri).build();
        try {
            Track track = getTrackRequest.execute();
            StringBuilder toSearch = new StringBuilder(track.getName());
            for (ArtistSimplified artist : track.getArtists()) {
                toSearch.append(" ").append(artist.getName());
            }
            youtube.play(toSearch.toString(), event, true);
        } catch (Exception e) {
            refreshCredentials();
            getTrack(uri, youtube, event);
        }
    }

    public void getPlaylist(String uri, YoutubeAudioManager youtube, MessageReceivedEvent event) {
        GetPlaylistsItemsRequest getPlaylistsItemsRequest = spotifyApi
                .getPlaylistsItems(uri)
                .build();
        try {
            Paging<PlaylistTrack> execute = getPlaylistsItemsRequest.execute();
            String next = "start";
            int i = 0;
            while (next != null) {
                next = execute.getNext();
                PlaylistTrack[] playlistTracks = execute.getItems();
                CompletableFuture.runAsync(() -> {
                    Main.playlistAdd = true;
                    Main.playlistLength += playlistTracks.length;
                    for (PlaylistTrack playlistTrack : playlistTracks) {
                        if (playlistTrack.getTrack() != null)
                            getTrack(playlistTrack.getTrack().getId(), youtube, event);
                    }
                });
                i += 100;
                GetPlaylistsItemsRequest getting = spotifyApi
                        .getPlaylistsItems(uri)
                        .offset(i)
                        .build();
                execute = getting.execute();
            }
        } catch (Exception e) {
            refreshCredentials();
        }
    }

    public void getAlbum(String uri, YoutubeAudioManager youtube, MessageReceivedEvent event) {
        GetAlbumsTracksRequest getAlbumItemsRequest = spotifyApi
                .getAlbumsTracks(uri)
                .build();
        try {
            TrackSimplified[] albumTracks = getAlbumItemsRequest.execute().getItems();
            Main.playlistLength = albumTracks.length;
            Main.playlistAdd = true;
            for (TrackSimplified albumTrack : albumTracks) {
                StringBuilder toSearch = new StringBuilder(albumTrack.getName());
                for (ArtistSimplified artist : albumTrack.getArtists()) {
                    toSearch.append(" ").append(artist.getName());
                }
                youtube.play(toSearch.toString(), event, true);
            }
        } catch (Exception e) {
            refreshCredentials();
            getAlbum(uri, youtube, event);
        }
    }
}
