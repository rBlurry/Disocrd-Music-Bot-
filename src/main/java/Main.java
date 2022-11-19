import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.security.auth.login.LoginException;

public class Main {
    public static String requestChannel = "song-requests";

    public static long requestChannelId = 1011760821946298450L;

    public static String queueTitle = "\r\n__**Queue list:**__\r\n";
    public static String queueDescription = "Join a voice channel and queue songs by name or url in here.\r\n";
    public static String noCurrentPlaying = "No song currently playing";
    public static String footerSb = " Current Queue | " + "0" + " entries | `" + "00:00" + "` ";

    public static String currentTrackTitle = noCurrentPlaying;
    public static String currentFooter = footerSb;
    public static StringBuilder fullList = new StringBuilder(queueDescription);

    public static MessageChannel messageChannel;

    public static Long userRblurry = 218566436908564480L;

    public static int playlistLength = 0;
    public static Boolean playlistAdd = false;

    public static long requestTimer = System.currentTimeMillis();
    public static long buttonTimer = System.currentTimeMillis();

    public static void main(String[] args) throws LoginException, IOException {

        BufferedReader brTest = new BufferedReader(new FileReader("./config.txt"));
        String token = brTest.readLine();

        JDABuilder builder = JDABuilder.createDefault(token);
        // Disable parts of the cache
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES);
        // Enable the bulk delete event
        builder.setBulkDeleteSplittingEnabled(false);
        // Disable compression (not recommended)
        builder.setCompression(Compression.NONE);
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS);
        // Set activity (like "playing Something")
//        builder.setActivity(Activity.listening(""));

        //Listeners
        builder.addEventListeners(new Listener('!'));
        builder.build();
    }
}
