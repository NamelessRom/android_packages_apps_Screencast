/*
 * <!--
 *    Copyright (C) 2014 The NamelessRom Project
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * -->
 */

package org.namelessrom.screencast.recording;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.DisplayMetrics;
import android.view.Surface;

import org.namelessrom.screencast.Logger;
import org.namelessrom.screencast.PreferenceHelper;
import org.namelessrom.screencast.Utils;

import java.io.IOException;
import java.util.ArrayList;

public abstract class EncoderDevice {
    private static final String VIRTUAL_DISPLAY_NAME = "hidden:screen-recording";

    private final Context mContext;

    private final int mWidth;
    private final int mHeight;

    private MediaCodec mMediaCodec;
    private VirtualDisplay mVirtualDisplay;

    public EncoderDevice(final Context context, final int width, final int height) {
        mContext = context;

        mWidth = width;
        mHeight = height;
    }

    private void destroyDisplaySurface(final MediaCodec codec) {
        if (codec != null) {
            codec.stop();
            codec.release();
        }

        if (mMediaCodec == codec) {
            mMediaCodec = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    public Surface createDisplaySurface() {
        final ArrayList<VideoEncoderCap> videoEncoderCaps = Utils.getVideoEncoderCaps();

        final VideoEncoderCap videoEncoderCap;
        if (videoEncoderCaps.size() > 0) {
            videoEncoderCap = videoEncoderCaps.get(0);
        } else {
            videoEncoderCap = null;
        }

        int bitRate = 2000000;
        if (videoEncoderCap != null) {
            bitRate = PreferenceHelper.get(mContext)
                    .getInt(PreferenceHelper.PREF_BITRATE, videoEncoderCap.maxBitRate);
            // cap max bitrate
            if (bitRate > videoEncoderCap.maxBitRate) {
                bitRate = videoEncoderCap.maxBitRate;
            }
            // cap min bitrate
            if (bitRate < videoEncoderCap.minBitRate) {
                bitRate = videoEncoderCap.minBitRate;
            }
            Logger.i(this, "bitRate -> %s", bitRate);
        }

        int frameRate = 30;
        if (videoEncoderCap != null) {
            frameRate = PreferenceHelper.get(mContext)
                    .getInt(PreferenceHelper.PREF_FRAMERATE, videoEncoderCap.maxFrameRate);
            // cap max framerate
            if (frameRate > videoEncoderCap.maxFrameRate) {
                frameRate = videoEncoderCap.maxFrameRate;
            }
            // cap min framerate
            if (frameRate < videoEncoderCap.minFrameRate) {
                frameRate = videoEncoderCap.minFrameRate;
            }
            Logger.i(this, "frameRate -> %s", frameRate);
        }

        final String mime = "video/avc";
        final MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime, mWidth, mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);

        Logger.i(this, String.format("Starting encoder at %sx%s", mWidth, mHeight));

        try {
            mMediaCodec = MediaCodec.createEncoderByType(mime);
        } catch (IOException io) {
            Logger.e(this, "could not create media codec!", io);
        }
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        final Surface inputSurface = mMediaCodec.createInputSurface();

        mMediaCodec.start();

        final EncoderRunnable encoderRunnable = onSurfaceCreated(mMediaCodec);
        final Thread encoderThread = new Thread(encoderRunnable, "Encoder");
        encoderThread.start();

        return inputSurface;
    }

    protected abstract EncoderRunnable onSurfaceCreated(final MediaCodec codec);

    public VirtualDisplay registerVirtualDisplay(final Context ctx, final DisplayMetrics metrics) {
        final Surface displaySurface = createDisplaySurface();
        if (displaySurface == null) { return null; }

        final DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
        final int displayFlags = DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE |
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
        final VirtualDisplay virtualDisplay = dm.createVirtualDisplay(VIRTUAL_DISPLAY_NAME,
                mWidth, mHeight, metrics.densityDpi, displaySurface, displayFlags);
        Logger.i(this, String.format("Created virtual display (%s)", displayFlags));
        mVirtualDisplay = virtualDisplay;

        return virtualDisplay;
    }

    public void stop() throws IllegalStateException {
        if (mMediaCodec != null) {
            mMediaCodec.signalEndOfInputStream();
            mMediaCodec = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    abstract class EncoderRunnable implements Runnable {
        /* package */ MediaCodec mediaCodec;

        abstract void encode() throws Exception;

        public EncoderRunnable(final MediaCodec mediaCodec) {
            this.mediaCodec = mediaCodec;
        }

        protected void cleanup() {
            EncoderDevice.this.destroyDisplaySurface(mediaCodec);
            this.mediaCodec = null;
        }

        public final void run() {
            try {
                encode();
                cleanup();
                Logger.i(EncoderDevice.this, "=======ENCODING COMPELTE=======");
            } catch (Exception e) {
                Logger.e(EncoderDevice.this, "Encoder error", e);
            }
        }
    }

}
