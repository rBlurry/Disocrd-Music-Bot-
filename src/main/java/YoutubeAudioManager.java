import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class YoutubeAudioManager {
    AudioPlayerManager playerManager;
    AudioPlayer youtube;
    TrackScheduler trackScheduler;

    public YoutubeAudioManager() {
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        youtube = playerManager.createPlayer();
        trackScheduler = new TrackScheduler(youtube, playerManager);
        youtube.addListener(trackScheduler);
    }

    public void play(String identifier, MessageReceivedEvent event, boolean isSpotify) {
        String song;
        if (!identifier.split("/")[0].contains("https")) {
            song = "ytsearch:" + identifier;
        } else {
            song = identifier;
        }
        playerManager.loadItem(song, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                track.setUserData(
                        new SongInfo(
                                event.getMember().getUser().getId(),
                                event.getMember().getEffectiveName()
                        )
                );
                trackScheduler.queue(track, youtube);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (song.contains("ytsearch:")) {
                    playlist.getTracks().get(0).setUserData(
                            new SongInfo(
                                    event.getMember().getUser().getId(),
                                    event.getMember().getEffectiveName()
                            )
                    );
                    trackScheduler.queue(playlist.getTracks().get(0), youtube);
                } else {
                    Main.playlistAdd = true;
                    Main.playlistLength = playlist.getTracks().size();
                    for(AudioTrack track : playlist.getTracks()) {
                        track.setUserData(
                                new SongInfo(
                                        event.getMember().getUser().getId(),
                                        event.getMember().getEffectiveName()
                                )
                        );
                        trackScheduler.queue(track,youtube);
                    }
                }
            }

            @Override
            public void noMatches() {
                System.out.println("no match");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.out.println("load failed");
            }
        });
    }

/*
    public void playFile(Message.Attachment file, MessageReceivedEvent event) {
        playerManager.loadItem(file.getProxyUrl(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                trackScheduler.queue(track, youtube);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                System.out.println("no playlist here");
            }

            @Override
            public void noMatches() {
                System.out.println("no match");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.out.println("load failed");
            }
        });
    }

 */

    public void playSoundcloud(String identifier, MessageReceivedEvent event) {
        playerManager.loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                track.setUserData(
                        new SongInfo(
                                event.getMember().getUser().getId(),
                                event.getMember().getEffectiveName()
                        )
                );
                trackScheduler.queue(track, youtube);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

            }

            @Override
            public void noMatches() {
                System.out.println("no match");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.out.println("load failed");
            }
        });
    }

    public void clean(MessageReceivedEvent event) {
        // Change what this does
    }

    public void skip(ButtonInteractionEvent event) {
        trackScheduler.nextTrack();
    }

    public void showQueue() {
        trackScheduler.showQueue();
        trackScheduler.clean();
    }

    public void setPlayPause() {
        trackScheduler.setPause();
    }

    public void stop() {
        trackScheduler.stop();
    }

    public void remove(int song) {
        trackScheduler.remove(song);
    }

    public Boolean isPlaying() {
        return youtube.getPlayingTrack() != null;
    }
}
