import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TrackScheduler extends AudioEventAdapter {
    private final BlockingQueue<AudioTrack> queue;
    private final AudioPlayer player;
    private final AudioPlayerManager playerManager;

    public static int counter = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Future<MessageEditAction> updateToDefaultUI() {
        Main.currentTrackTitle = Main.noCurrentPlaying;
        Main.currentFooter = Main.footerSb;

        MessageHistory history = MessageHistory.getHistoryFromBeginning(Main.messageChannel).complete();
        List<Message> filteredHistory = history.getRetrievedHistory().stream().filter(
                Message::isPinned
        ).collect(Collectors.toList());
        return executor.submit(() -> filteredHistory.get(0).editMessageEmbeds(
                new EmbedBuilder()
                        .setColor(Color.MAGENTA)
                        .setImage("attachment://botmain.webp")
                        .setTitle(Main.noCurrentPlaying)
                        .setFooter(Main.footerSb)
                        .build()
        )
                .setContent(Main.queueTitle)
                .setContent(Main.queueDescription));
    }

    public Future<MessageEditAction> updateToTrackUI() {
        if (queue.isEmpty()) {
            Main.fullList = new StringBuilder(Main.queueDescription);
        }
        MessageHistory history = MessageHistory.getHistoryFromBeginning(Main.messageChannel).complete();
        List<Message> filteredHistory = history.getRetrievedHistory().stream().filter(
                Message::isPinned
        ).collect(Collectors.toList());
        AudioTrack current = player.getPlayingTrack();
        Main.currentTrackTitle = "`[" + convertToTime(current.getInfo().length) + "]` **" + current.getInfo().title + "** - <@" + SongKt.getSongInfo(current).getAdderTitle() + ">";
        return executor.submit(() -> filteredHistory.get(0).editMessageEmbeds(
                new EmbedBuilder()
                        .setColor(Color.MAGENTA)
                        .setImage("attachment://botmain.webp")
                        .setTitle(Main.currentTrackTitle)
                        .setFooter(Main.currentFooter)
                        .build()
        )
                .setContent(Main.queueTitle)
                .setContent(String.valueOf(Main.fullList)));
    }

    public TrackScheduler(AudioPlayer player, AudioPlayerManager playerManager) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
        this.playerManager = playerManager;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
        if (queue.isEmpty()) {
            showQueue();
            try {
                updateToTrackUI().get().queue();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext && endReason != AudioTrackEndReason.LOAD_FAILED || endReason == AudioTrackEndReason.STOPPED) {
            if (!queue.isEmpty()) {
                player.startTrack(queue.poll(), false);
                showQueue();
                try {
                    updateToTrackUI().get().queue();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                player.startTrack(null, true);
                try {
                    updateToDefaultUI().get().queue();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } else if (endReason == AudioTrackEndReason.LOAD_FAILED) {
            //do nothing
        } else {
            player.startTrack(null, true);
            try {
                updateToDefaultUI().get().queue();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        //reloads track
        playerManager.loadItem(track.getIdentifier(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                player.startTrack(audioTrack, false);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                //do nothing
            }

            @Override
            public void noMatches() {
                //do nothing
            }

            @Override
            public void loadFailed(FriendlyException e) {
                //do nothing
            }
        });
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        System.out.println("stuck");
        nextTrack();
    }

    public void queue(AudioTrack audio, AudioPlayer player) {
        if (!player.startTrack(audio, true)) {
            counter = counter + 1;
            if (Main.playlistLength == 0) {
                System.out.println("Processing Single Song " + " [ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + " ] ");
            } else {
                System.out.println("Processing Song #" + counter + " of " + (Main.playlistLength-1) + " [ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + " ] ");
            }
            queue.add(audio);
            if (counter == 1) {
                System.out.println("Update UI at first song");
                updateToDefaultUI().cancel(true);
                updateToTrackUI().cancel(true);
                showQueue();
                try {
                    updateToTrackUI().get().queue();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            if (counter % 50 == 0) {
                System.out.println("Update UI at interval 50");
                updateToDefaultUI().cancel(true);
                updateToTrackUI().cancel(true);
                showQueue();
                try {
                    updateToTrackUI().get().queue();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            if (Main.playlistLength-1 == counter) {
                shuffle();
                Main.playlistAdd = false;
            }
            if (!Main.playlistAdd) {
                showQueue();
                System.out.println("Update UI at final song");
                updateToDefaultUI().cancel(true);
                updateToTrackUI().cancel(true);
                try {
                    updateToTrackUI().get().queue();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                counter = 0;
                Main.playlistLength = 0;
            }
        }
    }


    public void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        if (player.getPlayingTrack() == null)
            return;
        if (!queue.isEmpty()) {
            player.startTrack(null, false);
        } else {
            stop();
        }
    }

    public void showQueue() {
        StringBuilder response = new StringBuilder();
        long duration = 0;
        Object[] aux = queue.toArray();
        for (int i = 0; i < aux.length && i <= 9; i++) {
            AudioTrack track = (AudioTrack) aux[i];
            response.insert(0,(i + 1) + ". " + "`[" + convertToTime(track.getInfo().length) + "]` **" + track.getInfo().title + "** - <@" + SongKt.getSongInfo(track).getAdderQueue() + ">" + "\r\n");
            duration += track.getDuration();
        }
        if (queue.size() > 10) {
            int remaining = queue.size() - 10;
            response.insert(0,remaining + " more tracks in queue ...\r\n");
        }
        Main.currentFooter = " Current Queue | " + (queue.size()) + " entries | `" + convertToTime(duration) + "` ";
        Main.fullList = response;
        if (queue.size() == 0) {
            Main.fullList = new StringBuilder(Main.queueDescription);
        }
    }

    public void clearQueue() {
        if (!queue.isEmpty()) {
            queue.clear();
        }
    }

    public void setPause() {
        player.setPaused(!player.isPaused());
    }

    public void clean() {
        System.out.println(Main.fullList);

        queue.forEach(System.out::println);
        try {
            updateToDefaultUI().get().queue();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        clearQueue();
        try {
            updateToDefaultUI().get().queue();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        player.stopTrack();
    }

    public void shuffle() {
        if (!queue.isEmpty()) {
            LinkedList<AudioTrack> aux = new LinkedList<>();
            queue.drainTo(aux);
            Collections.shuffle(aux);
            queue.addAll(aux);
        }
    }

    public void remove(int song) {
        if (!queue.isEmpty() && song <= queue.size()) {
            LinkedList<AudioTrack> aux = new LinkedList<>();
            queue.drainTo(aux);
            aux.remove(song - 1);
            queue.addAll(aux);
            try {
                updateToTrackUI().get().queue();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    String convertToTime(Long time) {
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60;
        String secondsString;
        if (seconds < 10L) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }
        final long minute = TimeUnit.MILLISECONDS.toMinutes(time) % 60;
        String minutesString;
        if (minute < 10L) {
            minutesString = "0" + minute;
        } else {
            minutesString = "" + minute;
        }
        final long hours = TimeUnit.MILLISECONDS.toHours(time) % 24;
        String hoursString;
        if (hours < 10L) {
            hoursString = "0" + hours;
        } else {
            hoursString = "" + hours;
        }
        if (hoursString.equals("00")) {
            return (minutesString + ":" + secondsString);
        } else {
            return (hoursString + ":" + minutesString + ":" + secondsString);
        }
    }
}