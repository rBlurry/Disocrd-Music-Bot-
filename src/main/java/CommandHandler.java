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
}
