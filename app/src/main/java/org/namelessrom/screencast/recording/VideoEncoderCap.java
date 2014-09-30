package org.namelessrom.screencast.recording;

import org.namelessrom.screencast.Logger;
import org.xml.sax.Attributes;

public class VideoEncoderCap {
    public final int maxBitRate;
    public final int minBitRate;
    public final int maxFrameHeight;
    public final int maxFrameWidth;
    public final int maxFrameRate;
    public final int minFrameRate;

    public static final int MAX_VIDEO_BITRATE = 2000000;

    public static final int[] VIDEO_BITRATES = new int[]{
            64000,
            128000,
            256000,
            320000,
            640000,
            1280000,
            1512000,
            1796000,
            MAX_VIDEO_BITRATE,
            //10000000,
    };

    public static final int MAX_VIDEO_FRAMERATE = 30;

    public static final int[] VIDEO_FRAMERATES = new int[]{
            15,
            18,
            20,
            24,
            25,
            26,
            29,
            MAX_VIDEO_FRAMERATE,
    };

    public VideoEncoderCap(final Attributes attributes) {
        maxFrameWidth = parseInt(attributes.getValue("maxFrameWidth"));
        maxFrameHeight = parseInt(attributes.getValue("maxFrameHeight"));
        maxBitRate = parseInt(attributes.getValue("maxBitRate"));
        minBitRate = parseInt(attributes.getValue("minBitRate"));
        maxFrameRate = parseInt(attributes.getValue("maxFrameRate"));
        minFrameRate = parseInt(attributes.getValue("minFrameRate"));
    }

    private int parseInt(final String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exc) {
            Logger.e(this, "parseInt", exc);
            return -1;
        }
    }
}
