import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayHandler {

    private static final SpotifyAPI spotifyApi = new SpotifyAPI();
    private static final Map<String, YoutubeAudioManager> audioManagers = new HashMap<>();

    public static void handlePlay(MessageReceivedEvent event, String song) {
        System.out.println("HandlePlay event called");
        AudioChannel connectedChannel = event.getMember().getVoiceState().getChannel();

        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        }
        YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
        AudioManager audioManager = event.getGuild().getAudioManager();
        if (audioManager.isConnected() && !audioManager.getConnectedChannel().equals(connectedChannel)) {

            return;
        } else {
            audioManager.openAudioConnection(connectedChannel);
            audioManager.setSendingHandler(new AudioPlayerSendHandler(youtube.youtube));
        }
        if (song.contains("open.spotify.com")) {
            System.out.println("Play Spotify");
            String[] removeQuery = song.split("\\?");
            String[] uriParts = removeQuery[0].split("/");
            if (uriParts[3].compareTo("track") == 0) {
                spotifyApi.getTrack(uriParts[4], youtube, event);
            } else if (uriParts[3].compareTo("playlist") == 0) {
                spotifyApi.getPlaylist(uriParts[4], youtube, event);
            } else if (uriParts[3].compareTo("album") == 0) {
                spotifyApi.getAlbum(uriParts[4], youtube, event);
            } else {
                System.out.println("Not Supported");
            }
        } else if (song.contains("https://soundcloud.com")) {
            System.out.println("Play soundcloud");
            youtube.playSoundcloud(song, event);
        } else {
            System.out.println("Play Youtube");
            youtube.play(song, event, false);
        }
    }

    public static void handleBlurryPlay(MessageReceivedEvent event, String song) {
        AudioChannel connectedChannel = null;
        Guild guild = event.getGuild();
        List<GuildVoiceState> memberVoiceStates = guild.getVoiceStates();
        for (GuildVoiceState member : memberVoiceStates) {
            if (member.inAudioChannel() && member.getChannel() != guild.getAfkChannel()) {
                connectedChannel = member.getChannel();
            }
        }
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        }
        YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
        AudioManager audioManager = event.getGuild().getAudioManager();
        try {
            if (audioManager.isConnected() && !audioManager.getConnectedChannel().equals(connectedChannel)) {
                return;
            } else if (!audioManager.isConnected()) {
                audioManager.openAudioConnection(connectedChannel);
                audioManager.setSendingHandler(new AudioPlayerSendHandler(youtube.youtube));
            }
            if (song.contains("open.spotify.com")) {
                String[] removeQuery = song.split("\\?");
                String[] uriParts = removeQuery[0].split("/");
                if (uriParts[3].compareTo("track") == 0) {
                    spotifyApi.getTrack(uriParts[4], youtube, event);
                } else if (uriParts[3].compareTo("playlist") == 0) {
                    spotifyApi.getPlaylist(uriParts[4], youtube, event);
                } else if (uriParts[3].compareTo("album") == 0) {
                    spotifyApi.getAlbum(uriParts[4], youtube, event);
                } else {
                    System.out.println("Not Supported");
                }
            } else if (song.contains("https://soundcloud.com")) {
                youtube.playSoundcloud(song, event);
            } else {
                youtube.play(song, event, false);
            }
        } catch (NullPointerException ignored) {

        }
    }

    public static void handleSkip(ButtonInteractionEvent event) {
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            System.out.println("No audio manager, don't process button click");
        } else {
            YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
            AudioManager audioManager = event.getGuild().getAudioManager();
            if (!audioManager.isConnected() || !youtube.isPlaying()) {
                System.out.println("Bot not connected or playing, don't process button click");
                event.deferEdit().queue();
                return;
            }
            youtube.skip(event);
        }
        event.deferEdit().queue();
    }

    public static void handlePlayPause(ButtonInteractionEvent event) {
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            System.out.println("No audio manager or playing, don't process button click");
            event.deferEdit().queue();
            return;
        } else {
            YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
            AudioManager audioManager = event.getGuild().getAudioManager();
            if (!audioManager.isConnected() || !youtube.isPlaying()) {
                System.out.println("Bot not connected or playing, don't process button click");
                event.deferEdit().queue();
                return;
            }
            youtube.setPlayPause();
        }
        event.deferEdit().queue();
    }

    public static void handleClean(ButtonInteractionEvent event) {
        if (event.getMember().getUser().getIdLong() != Main.userRblurry) {
            if (!audioManagers.containsKey(event.getGuild().getId())) {
                System.out.println("No audio manager, don't process button click");
                event.deferEdit().queue();
                return;
            } else {
                YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
                AudioManager audioManager = event.getGuild().getAudioManager();
                if (!audioManager.isConnected() || !youtube.isPlaying()) {
                    System.out.println("Bot not connected or playing, don't process button click");
                    event.deferEdit().queue();
                    return;
                }
                youtube.showQueue();
            }
        } else {
            if (!audioManagers.containsKey(event.getGuild().getId())) {
                audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
            }
            YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
            youtube.showQueue();
        }
        event.deferEdit().queue();
    }

    public static void handleStop(ButtonInteractionEvent event) {
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            System.out.println("No audio manager, don't process button click");
            event.deferEdit().queue();
            return;
        }  else {
            YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
            AudioManager audioManager = event.getGuild().getAudioManager();
            if (!audioManager.isConnected() || !youtube.isPlaying()) {
                System.out.println("Bot not connected or playing, don't process button click");
                event.deferEdit().queue();
                return;
            }
            youtube.stop();
        }
        event.deferEdit().queue();
    }

    public static void leaveStop(GuildVoiceUpdateEvent event) {
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            System.out.println("No audio manager, don't process button click");
        } else {
            YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
            youtube.stop();
        }
    }

    public static void handleRemove(MessageReceivedEvent event, int song) {
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        } else {
            YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
            AudioManager audioManager = event.getGuild().getAudioManager();
            if (!audioManager.isConnected() || !youtube.isPlaying()) {
                System.out.println("Bot not connected or playing, don't process button click");
                return;
            }
            youtube.remove(song);
        }
    }
}
