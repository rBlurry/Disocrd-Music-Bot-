import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.nio.file.Path;

public class CommandHandler {

    public static void handleSetup(MessageReceivedEvent event) {

        try {
            event.getGuild().getTextChannelsByName("song-requests", true).get(0);
        } catch (Exception ignored) {
            FileUpload file = FileUpload.fromData((Path) CommandHandler.class.getResourceAsStream("src/main/assets/_banner2.webp"));
            TextChannel getChannel = event.getGuild().getTextChannelById(createTextChannel(event.getGuild()));
            getChannel.sendFiles(file).complete();
            setupQueuePlayer(getChannel);
        }
    }

    private static String createTextChannel(Guild guild) {
        return guild.createTextChannel(Main.requestChannel)
                .setTopic("\r\n\t" +
                        "\r\n\t" + "Audio Interface for The Boys Bot. " +
                        "\r\n\t" + "DO NOT CLOSE THE EMBED!!!" +
                        "\r\n\t" + "Use !remove # to remove a specific song from queue."
                )
                .complete()
                .getId();
    }

    private static void setupQueuePlayer(TextChannel channel) {
        FileUpload file = FileUpload.fromData((Path) CommandHandler.class.getResourceAsStream("src/main/assets/botmain.webp"));
        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setColor(Color.MAGENTA)
                        .setImage("attachment://botmain.webp")
                        .setTitle(Main.noCurrentPlaying)
                        .setFooter(Main.footerSb)
                        .build()
        )
                .setActionRow(
                        Button.success("btnA", "Play/Pause"),
                        Button.danger("btnB", "Stop"),
                        Button.primary("btnC", "Skip"),
                        Button.secondary("btnD", "Clean")
                )
                .addFiles(file)
                .setContent(Main.queueTitle)
                .setContent(Main.queueDescription)
                .complete();
    }

//    public static void checkToLeave(GuildVoiceUpdateEvent event) {
//        try {
//            int connected = event.getGuild().getAudioManager().getConnectedChannel().getMembers().size();
//            if (connected == 1) {
//                System.out.println("Bot is alone, leaving");
//                try {
//                    leave(event);
//                } catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//            }
//            }
//        } catch (Exception ignored) {
//        }
//    }

//    private ExecutorService executor = Executors.newSingleThreadExecutor();
//
//    public static void leave(GuildVoiceUpdateEvent event) {
//        event.getGuild().getAudioManager().closeAudioConnection();
//        Main.currentTrackTitle = Main.noCurrentPlaying;
//        Main.currentFooter = Main.footerSb;
//        Main.fullList = new StringBuilder(Main.queueDescription);
//
//        MessageHistory history = MessageHistory.getHistoryFromBeginning(Main.messageChannel).complete();
//        List<Message> filteredHistory = history.getRetrievedHistory().stream().filter(
//                Message::isPinned
//        ).collect(Collectors.toList());
//        executor.submit(() -> filteredHistory.get(0).editMessageEmbeds(
//                new EmbedBuilder()
//                        .setColor(Color.MAGENTA)
//                        .setImage("attachment://botmain.webp")
//                        .setTitle(Main.noCurrentPlaying)
//                        .setFooter(Main.footerSb)
//                        .build()
//        )
//                .append(Main.queueTitle)
//                .append(Main.queueDescription));
//    }


/*
    public static void handleFile(MessageReceivedEvent event, Message.Attachment attachment) {
        VoiceChannel connectedChannel = event.getMember().getVoiceState().getChannel();
        if (connectedChannel == null) {
            handleResponse(event, "You are not in a voice channel!");
            return;
        }
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        }
        YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
        AudioManager audioManager = event.getGuild().getAudioManager();
        if (audioManager.isConnected() && !audioManager.getConnectedChannel().equals(connectedChannel)) {
            handleResponse(event, "The bot is already connected to a voice channel.");
            return;
        } else {
            audioManager.openAudioConnection(connectedChannel);
            audioManager.setSendingHandler(new AudioPlayerSendHandler(youtube.youtube));
        }
        youtube.playFile(attachment, event);
    }

    public static void handleDisconnect(MessageReceivedEvent event) {
        VoiceChannel connectedChannel = event.getMember().getVoiceState().getChannel();
        if (connectedChannel == null) {
            handleResponse(event, "You are not in a voice channel!");
            return;
        }
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        }
        YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
        AudioManager audioManager = event.getGuild().getAudioManager();
        if (!audioManager.isConnected() && audioManager.getConnectedChannel() != connectedChannel) {
            handleResponse(event, "The bot is not connected to a voice channel.");
            return;
        }
        // Connects to the channel.
        youtube.clean(event);
        audioManager.closeAudioConnection();
        handleResponse(event, "Bot disconnected!");
    }

    public static void handleQueue(MessageReceivedEvent event) {
        VoiceChannel connectedChannel = event.getMember().getVoiceState().getChannel();
        if (connectedChannel == null) {
            handleResponse(event, "You are not in a voice channel!");
            return;
        }
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        }
        YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
        youtube.showQueue(event);
    }

    public static void handleStop(MessageReceivedEvent event) {
        VoiceChannel connectedChannel = event.getMember().getVoiceState().getChannel();
        if (connectedChannel == null) {
            handleResponse(event, "You are not in a voice channel!");
            return;
        }
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        }
        YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
        youtube.stop(event);
    }

    public static void handleShuffle(MessageReceivedEvent event) {
        VoiceChannel connectedChannel = event.getMember().getVoiceState().getChannel();
        if (connectedChannel == null) {
            handleResponse(event, "You are not in a voice channel!");
            return;
        }
        AudioManager audioManager = event.getGuild().getAudioManager();
        if (!audioManager.isConnected() && audioManager.getConnectedChannel() != connectedChannel) {
            handleResponse(event, "The bot is not connected to a voice channel.");
            return;
        }
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        }
        YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
        youtube.shuffle(event);
    }

    public static void handleRemove(MessageReceivedEvent event, String toRemove) {
        int song = 0;
        try {
            song = Integer.parseInt(toRemove);
        } catch (NumberFormatException ex) {
            handleResponse(event, "Input a number.");
        }
        if (song < 1 || song > 10) {
            handleResponse(event, "Input a number between 1-10.");
            return;
        }
        VoiceChannel connectedChannel = event.getMember().getVoiceState().getChannel();
        if (connectedChannel == null) {
            handleResponse(event, "You are not in a voice channel!");
            return;
        }
        if (!audioManagers.containsKey(event.getGuild().getId())) {
            audioManagers.put(event.getGuild().getId(), new YoutubeAudioManager());
        }
        YoutubeAudioManager youtube = audioManagers.get(event.getGuild().getId());
        AudioManager audioManager = event.getGuild().getAudioManager();
        if (!audioManager.isConnected() && audioManager.getConnectedChannel() != connectedChannel) {
            handleResponse(event, "The bot is not connected to a voice channel.");
            return;
        }
        youtube.remove(event, song);
    }

 */
}
