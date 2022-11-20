import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Listener extends ListenerAdapter {
    private final char prefix;

    public Listener(char prefix) {
        this.prefix = prefix;
    }

    @Override
    public void onReady(ReadyEvent event) {
        Main.messageChannel = event.getJDA().getTextChannelById(Main.requestChannelId);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String user = event.getAuthor().getName();
        if (event.getAuthor().isBot()) {
            return;
        }

        if ((event.getChannel().getType() == ChannelType.TEXT && (event.getChannel().getIdLong() == Main.requestChannelId))
                || message.startsWith("!setup")
        ) {
            //////////////////////// LOG THE USER INFO /////////////////////
            System.out.println(
                    "[ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + " ] - " +
                            "\r\n\t Message received: " + message +
                            "\r\n\t From: " + user +
                            "\r\n\t"
            );
            /////////////////////// BLOCK UNWANTED MESSAGES /////////////////////
            if (message.contains("results?search")) {
                System.out.println("Search Query, WTF WHY MIKE!?!??!");
                event.getMessage().delete().queue();
                return;
            }
            if (!event.getMessage().getAttachments().isEmpty()) {
                System.out.println("Message with attachment! Get the Fuck out!");
                event.getMessage().delete().queue();
                return;
            }
            if (message.contains("https://tenor.com/")) {
                System.out.println("GIF Message! Get the Fuck out!");
                event.getMessage().delete().queue();
                return;
            }
            if (message.contains("com/short")) {
                System.out.println("Short! Get the Fuck out!");
                event.getMessage().delete().queue();
                return;
            }
            if (message.contains("discordapp.com/")) {
                System.out.println("DISOCRD! Get the Fuck out!");
                event.getMessage().delete().queue();
                return;
            }
            if (message.equals("?")) {
                System.out.println("WTF! Get the Fuck out!");
                event.getMessage().delete().queue();
                return;
            }
            if (message.contains("<@!")) {
                System.out.println("No Atting people");
                event.getMessage().delete().queue();
                return;
            }
            if (!event.getMember().getVoiceState().inAudioChannel() && event.getMember().getUser().getIdLong() != Main.userRblurry) {
                System.out.println(user + " Attempted to play a song while not in VC");
                event.getMessage().delete().queue();
                return;
            }
            if (event.getMember().getVoiceState().getChannel() == event.getGuild().getAfkChannel()) {
                System.out.println("Attempted to join Afk Channel");
                event.getMessage().delete().queue();
                return;
            }
            //////////////////////// DELETE THE USED MESSAGE /////////////////////
            event.getChannel().purgeMessagesById(event.getChannel().getLatestMessageId());
            //////////////////////// RUN SETUP IF FOUND //////////////////////////
            if (message.startsWith("!setup")) {
                System.out.println(user + " just ran the setup command");
                CommandHandler.handleSetup(event);
                return;
            }
            if (message.startsWith("!remove")) {
                String songNumber = message.substring(8);
                System.out.println(user + " wants to remove song at position " + songNumber);
                PlayHandler.handleRemove(event, Integer.parseInt(songNumber));
                return;
            }
            AudioChannel current = event.getGuild().getSelfMember().getVoiceState().getChannel();
            GuildVoiceState userState = event.getMember().getVoiceState();
            if (event.getMember().getUser().getIdLong() != Main.userRblurry) {
                if (!userState.inAudioChannel() || current != null && userState.getChannel() != current) {
                    System.out.println(user + " just tried to play a song while in bad VC state.");
                    return;
                }
            }
            //////////////////////// BEGIN ADD SONG//////////////////////////
            if (!event.getMember().getVoiceState().inAudioChannel() && event.getMember().getUser().getIdLong() == Main.userRblurry) {
                if (Main.playlistAdd) {
                    FileUpload file = FileUpload.fromData((Path) CommandHandler.class.getResourceAsStream("src/main/assets/monkee.png"));
                    event.getMember().getUser().openPrivateChannel()
                            .flatMap(channel -> channel.sendMessage("I am currently processing song #"
                                    + TrackScheduler.counter
                                    + " of #" + Main.playlistLength
                                    + ", please do not attempt to add anymore songs to the queue at this time.")
                                    .addFiles(file))
                            .queue();
                    return;
                } else {
                    checkRequestTimer(event);
                    PlayHandler.handleBlurryPlay(event, message);
                }
                return;
            }
            PlayHandler.handlePlay(event, message);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        User user = event.getMember().getUser();
        if ((user.getIdLong() != Main.userRblurry && !event.getMember().getVoiceState().inAudioChannel())
                || (user.getIdLong() != Main.userRblurry && event.getGuild().getSelfMember().getVoiceState() == null)
                || (user.getIdLong() != Main.userRblurry && event.getMember().getVoiceState().getChannel() != event.getGuild().getSelfMember().getVoiceState().getChannel())
        ) {
            System.out.println(user.getName() + " just tried to hit the ${event.button?.label} button while in bad VC state.");
            return;
        }
        if (!event.getButton().getLabel().isEmpty()) {
            switch (event.getButton().getLabel()) {
                case ("Stop"):
                    System.out.println("[ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + " ] - " + "STOP button pressed by: " + user.getName());
                    checkButtonTimer(event, "Stop");
                    PlayHandler.handleStop(event);
                    break;
                case ("Play/Pause"):
                    System.out.println("[ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + " ] - " + "PLAY/PAUSE button pressed by: " + user.getName());
                    checkButtonTimer(event, "Play/Pause");
                    PlayHandler.handlePlayPause(event);
                    break;
                case ("Clean"):
                    System.out.println("[ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + " ] - " + "CLEAN button pressed by: " + user.getName());
                    checkButtonTimer(event, "Clean");
                    PlayHandler.handleClean(event);
                    break;
                case ("Skip"):
                    System.out.println("[ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + " ] - " + "SKIP button pressed by: " + user.getName());
                    checkButtonTimer(event, "Skip");
                    PlayHandler.handleSkip(event);
                    break;
            }
        }
    }

    private void checkButtonTimer(ButtonInteractionEvent event, String button) {
        try {
            long possibleDelay = System.currentTimeMillis() - 2600;
            if (possibleDelay <= Main.buttonTimer) {
                System.out.println("Delay processing by: " + (Main.buttonTimer - possibleDelay) + " milliseconds");
                event.getMember().getUser().openPrivateChannel()
                        .flatMap(channel -> channel.sendMessage("Delayed processing " + button + " to prevent spam."))
                        .queue();
                TimeUnit.MILLISECONDS.sleep(Main.buttonTimer - possibleDelay);
            } else {
                System.out.println("Handle button normally");
            }
            Main.buttonTimer = System.currentTimeMillis();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void checkRequestTimer(MessageReceivedEvent event) {
        try {
            long possibleDelay = System.currentTimeMillis() - 2600;
            if (possibleDelay <= Main.requestTimer) {
                System.out.println("Delay processing by: " + (Main.requestTimer - possibleDelay) + " milliseconds");
                event.getMember().getUser().openPrivateChannel()
                        .flatMap(channel -> channel.sendMessage("Delayed processing song request for to prevent spam"))
                        .queue();
                TimeUnit.MILLISECONDS.sleep(Main.requestTimer - possibleDelay);
            } else {
                System.out.println("Handle player normally");
            }
            Main.requestTimer = System.currentTimeMillis();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        System.out.println("[ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + " ] - " + "Voice event: " + event.getMember().getUser().getName() + " changed their state to " + event);
        int connected;
        try {
            connected = event.getGuild().getAudioManager().getConnectedChannel().getMembers().size();
        } catch (NullPointerException e) {
            connected = 0;
        }
        if (connected == 1) {
            System.out.println("Bot is alone, leaving");
            PlayHandler.leaveStop(event);
            event.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    @Override
    public void onGuildVoiceGuildDeafen(GuildVoiceGuildDeafenEvent event) {
        if (event.getGuild().getSelfMember().getUser().isBot()) {
            try {
                if (!event.getGuild().getSelfMember().getVoiceState().isGuildDeafened()) {
                    System.out.println("I AM UNDEAFENED! DON'T DO THAT!");
                    event.getGuild().getSelfMember().deafen(true).queue();
                }
            } catch (Exception e) {
                System.out.println("General Error during deafen step");
            }
        }
    }

}
