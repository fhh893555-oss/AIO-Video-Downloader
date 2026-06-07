package sysModules.player.mediaitem;

import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

public final class MediaItemTag {

    private final int serviceId;
    private final int videoIndex;
    private final int audioIndex;
    @Nullable private final VideoStream selectedVideo;
    @Nullable private final AudioStream selectedAudio;

    public MediaItemTag(final int serviceId,
                        final int videoIndex,
                        final int audioIndex,
                        @Nullable final VideoStream selectedVideo,
                        @Nullable final AudioStream selectedAudio) {
        this.serviceId = serviceId;
        this.videoIndex = videoIndex;
        this.audioIndex = audioIndex;
        this.selectedVideo = selectedVideo;
        this.selectedAudio = selectedAudio;
    }

    public int getServiceId() {
        return serviceId;
    }

    public int getVideoIndex() {
        return videoIndex;
    }

    public int getAudioIndex() {
        return audioIndex;
    }

    @Nullable
    public VideoStream getSelectedVideo() {
        return selectedVideo;
    }

    @Nullable
    public AudioStream getSelectedAudio() {
        return selectedAudio;
    }
}
