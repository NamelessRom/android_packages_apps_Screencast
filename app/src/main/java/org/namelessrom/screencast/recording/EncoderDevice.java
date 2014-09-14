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
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.text.TextUtils;
import android.view.Surface;

import org.namelessrom.screencast.Logger;
import org.xml.sax.Attributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;

import safesax.Parsers;

public abstract class EncoderDevice {
    final String TAG = getClass().getSimpleName();

    private MediaCodec     mMediaCodec;
    private VirtualDisplay mVirtualDisplay;

    private int mWidth;
    private int mHeight;

    public EncoderDevice(final int width, final int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    private void destroyDisplaySurface(final MediaCodec codec) {
        try {
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
        } catch (Exception e) {
            Logger.e(TAG, "Error destroying display surface", e);
        }
    }

    public Surface createDisplaySurface() {
        try {
            final ArrayList<VideoEncoderCap> videoEncoderCaps = new ArrayList<VideoEncoderCap>();
            try {
                File localFile = new File("/system/etc/media_profiles.xml");
                FileInputStream localFileInputStream = new FileInputStream(localFile);
                byte[] arrayOfByte = new byte[(int) localFile.length()];
                localFileInputStream.read(arrayOfByte);
                String str = new String(arrayOfByte);
                RootElement localRootElement = new RootElement("MediaSettings");
                Element localElement = localRootElement.requireChild("VideoEncoderCap");
                ElementListener local1 = new ElementListener() {
                    public void end() { }

                    public void start(Attributes attributes) {
                        if (TextUtils.equals(attributes.getValue("name"), "h264")) {
                            videoEncoderCaps.add(new EncoderDevice.VideoEncoderCap(attributes));
                        }
                    }
                };
                localElement.setElementListener(local1);
                StringReader localStringReader = new StringReader(str);
                Parsers.parse(localStringReader, localRootElement.getContentHandler());
            } catch (Exception localException1) {
                Logger.e(TAG, "Uhoh", localException1);
            }

            final VideoEncoderCap videoEncoderCap;
            if (videoEncoderCaps.size() > 0) {
                videoEncoderCap = videoEncoderCaps.get(0);
            } else {
                videoEncoderCap = null;
            }

            // TODO: make configurable to reduce screen cast size
            int bitRate = 2000000;
            if (videoEncoderCap != null) {
                bitRate = videoEncoderCap.maxBitRate;
            }

            // TODO: make configurable
            int frameRate = 30;
            if (videoEncoderCap != null) {
                frameRate = videoEncoderCap.maxFrameRate;
            }

            final MediaFormat mediaFormat = MediaFormat.createVideoFormat(
                    "video/avc", mWidth, mHeight);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);

            Logger.i(TAG, "Starting encoder at " + mWidth + "x" + mHeight);

            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
            mMediaCodec.configure(mediaFormat, null, null, 1);

            final Surface localSurface = mMediaCodec.createInputSurface();

            mMediaCodec.start();

            final EncoderRunnable encoderRunnable = onSurfaceCreated(mMediaCodec);
            Thread localThread = new Thread(encoderRunnable, "Encoder");
            localThread.start();

            return localSurface;
        } catch (Exception e) {
            Logger.e(TAG, "error creating surface", e);
        }

        return null;
    }

    protected abstract EncoderRunnable onSurfaceCreated(final MediaCodec codec);

    public VirtualDisplay registerVirtualDisplay(final Context ctx, final String displayName,
            final int width, final int height, final int dpi) {
        final Surface localSurface = createDisplaySurface();
        if (localSurface == null) { return null; }

        final DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
        final int displayFlags = DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE |
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
        final VirtualDisplay virtualDisplay = dm.createVirtualDisplay(
                displayName, this.mWidth, this.mHeight, dpi, localSurface, displayFlags);
        Logger.i(TAG, String.format("Created virtual display (%s)", displayFlags));
        mVirtualDisplay = virtualDisplay;

        return virtualDisplay;
    }

    public void stop() {
        try {
            if (mMediaCodec != null) {
                mMediaCodec.signalEndOfInputStream();
                mMediaCodec = null;
            }
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }
        } catch (Exception e) {
            Logger.e(TAG, "Stop", e);
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
                Logger.i(EncoderDevice.this.TAG, "=======ENCODING COMPELTE=======");
            } catch (Exception localException) {
                Logger.e(EncoderDevice.this.TAG, "Encoder error", localException);
            }
        }
    }

    private static class VideoEncoderCap {
        final int maxBitRate;
        final int maxFrameHeight;
        final int maxFrameWidth;
        final int maxFrameRate;

        public VideoEncoderCap(final Attributes attributes) {
            maxFrameWidth = Integer.valueOf(attributes.getValue("maxFrameWidth"));
            maxFrameHeight = Integer.valueOf(attributes.getValue("maxFrameHeight"));
            maxBitRate = Integer.valueOf(attributes.getValue("maxBitRate"));
            maxFrameRate = Integer.valueOf(attributes.getValue("maxFrameRate"));
        }
    }
}
