package sysModules.player.mediaitem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.Collections;
import java.util.List;

public final class MediaItemTag {

    private final int serviceId;
    private final int videoIndex;
    private final int audioIndex;
    @Nullable private final VideoStream selectedVideo;
    @Nullable private final AudioStream selectedAudio;
    @NonNull private final List<VideoStream> sortedVideoStreams;
    @NonNull private final List<AudioStream> sortedAudioStreams;

    public MediaItemTag(final int serviceId,
                        final int videoIndex,
                        final int audioIndex,
                        @Nullable final VideoStream selectedVideo,
                        @Nullable final AudioStream selectedAudio,
                        @NonNull final List<VideoStream> sortedVideoStreams,
                        @NonNull final List<AudioStream> sortedAudioStreams) {
        this.serviceId = serviceId;
        this.videoIndex = videoIndex;
        this.audioIndex = audioIndex;
        this.selectedVideo = selectedVideo;
        this.selectedAudio = selectedAudio;
        this.sortedVideoStreams = sortedVideoStreams;
        this.sortedAudioStreams = sortedAudioStreams;
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

    @NonNull
    public List<VideoStream> getSortedVideoStreams() {
        return sortedVideoStreams;
    }

    @NonNull
    public List<AudioStream> getSortedAudioStreams() {
        return sortedAudioStreams;
    }
}
