import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

public final class SongInfo {
    @NotNull
    private final String adderQueue;
    @NotNull
    private final String adderTitle;

    @NotNull
    public final String getAdderQueue() {
        return this.adderQueue;
    }

    @NotNull
    public final String getAdderTitle() {
        return this.adderTitle;
    }

    public SongInfo(@NotNull String adderQueue, @NotNull String adderTitle) {
        super();
        Intrinsics.checkNotNullParameter(adderQueue, "adderQueue");
        Intrinsics.checkNotNullParameter(adderTitle, "adderTitle");
        this.adderQueue = adderQueue;
        this.adderTitle = adderTitle;
    }
}

final class SongKt {
    @NotNull
    public static SongInfo getSongInfo(@NotNull AudioTrack audioTrack) {
        return audioTrack.getUserData(SongInfo.class);
    }
}
