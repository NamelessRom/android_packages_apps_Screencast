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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Environment;

import org.namelessrom.screencast.Logger;
import org.namelessrom.screencast.PreferenceHelper;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;

public class RecordingDevice extends EncoderDevice {
    private static final File RECORDINGS_DIR = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Screencasts");

    private final Context mContext;
    private final File mFile;

    public RecordingDevice(final Context context, final int width, final int height) {
        super(context, width, height);
        mContext = context;

        final String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
                .format(new Date(System.currentTimeMillis()));
        mFile = new File(RECORDINGS_DIR, "Screencast_" + date + ".mp4");
    }

    public String getRecordingFilePath() {
        return mFile.getAbsolutePath();
    }

    protected EncoderDevice.EncoderRunnable onSurfaceCreated(final MediaCodec mediaCodec) {
        return new Recorder(mediaCodec);
    }

    private class AudioMuxer implements Runnable {
        private final RecordingDevice.AudioRecorder recorder;

        private final Semaphore muxWaiter;
        private final MediaMuxer muxer;

        private int track;

        public AudioMuxer(final RecordingDevice.AudioRecorder audio, final MediaMuxer mediaMuxer,
                final Semaphore semaphore) {
            recorder = audio;
            muxer = mediaMuxer;
            muxWaiter = semaphore;
        }

        void encode() {
            ByteBuffer[] outputBuffers = recorder.codec.getOutputBuffers();
            final long l = System.nanoTime();

            while (true) {
                final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                final int status = recorder.codec.dequeueOutputBuffer(bufferInfo, -1L);
                if (status >= 0) {
                    final ByteBuffer byteBuffer = outputBuffers[status];
                    bufferInfo.presentationTimeUs = ((System.nanoTime() - l) / 1000L);
                    muxer.writeSampleData(track, byteBuffer, bufferInfo);
                    recorder.codec.releaseOutputBuffer(status, false);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Logger.d(this, "end of stream reached");
                        break;
                    }
                } else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = recorder.codec.getOutputBuffers();
                } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    final MediaFormat mediaFormat = recorder.codec.getOutputFormat();
                    track = muxer.addTrack(mediaFormat);
                    muxer.start();
                    muxWaiter.release();
                }
            }
        }

        public void run() {
            if (recorder.record.getState() != 1) {
                muxer.start();
                return;
            }

            try {
                encode();
            } catch (Exception exc) {
                Logger.e("RecordingDevice", "Audio Muxer error", exc);
            } finally {
                Logger.i("RecordingDevice", "AudioMuxer done");
                muxWaiter.release();
            }
        }
    }

    private class AudioRecorder implements Runnable {
        private static final String MIME = "audio/mp4a-latm";

        private final RecordingDevice.Recorder recorder;

        private final MediaCodec codec;
        private AudioRecord record;

        private NoiseSuppressor noiseSuppressor;

        public AudioRecorder(final RecordingDevice.Recorder recorder) throws IOException {
            this.recorder = recorder;

            final MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, MIME);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 64 * 1024);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectHE);

            codec = MediaCodec.createEncoderByType(MIME);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            // TODO: remote submix?
            final int minBuffer = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            Logger.i("RecordingDevice", "AudioRecorder init: " + String.valueOf(minBuffer));
            record = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer);

            // check if noise suppression is available
            if (NoiseSuppressor.isAvailable()) {
                // if it is available, check if the user turned it on
                final boolean useNoiseSuppression = PreferenceHelper.get(mContext)
                        .getBoolean(PreferenceHelper.PREF_ENABLE_NOISE_SUPPRESSION, false);
                if (useNoiseSuppression) {
                    noiseSuppressor = NoiseSuppressor.create(record.getAudioSessionId());
                    noiseSuppressor.setEnabled(true);
                }
            }
        }

        void encode() {
            final ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer byteBuffer;
            while (!recorder.doneCoding) {
                final int status = codec.dequeueInputBuffer(1024L);
                if (status >= 0) {
                    byteBuffer = inputBuffers[status];
                    byteBuffer.clear();
                    int number = record.read(byteBuffer, byteBuffer.capacity());
                    if (number < 0) {
                        number = 0;
                    }
                    byteBuffer.clear();
                    codec.queueInputBuffer(status, 0, number, System.nanoTime() / 1000L, 0);
                }
            }
            final int index = codec.dequeueInputBuffer(-1L);
            codec.queueInputBuffer(index, 0, 0, System.nanoTime() / 1000L, 4);
        }

        public final void run() {
            try {
                record.startRecording();
                encode();
                record.stop();
                record.release();
                record = null;
                if (noiseSuppressor != null) {
                    noiseSuppressor.release();
                    noiseSuppressor = null;
                }
                Logger.i("RecordingDevice", "=======RECORDING COMPLETE=======");
            } catch (Exception e) {
                Logger.e("RecordingDevice", "Recorder error", e);
            }
        }

    }

    private class Recorder extends EncoderDevice.EncoderRunnable {
        boolean doneCoding = false;

        public Recorder(MediaCodec mediaCodec) {
            super(mediaCodec);
        }

        protected void cleanup() {
            super.cleanup();
            doneCoding = true;
        }

        public void encode() throws Exception {
            Logger.v(this, "Created directory: %s", mFile.getParentFile().mkdirs());

            final MediaMuxer mediaMuxer = new MediaMuxer(
                    mFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int formatStatus = 0;
            int trackIndex = -1;
            Thread audioMuxerThread = null;
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

            final boolean withAudio = PreferenceHelper.get(mContext)
                    .getBoolean(PreferenceHelper.PREF_ENABLE_AUDIO, true);

            final long start = System.nanoTime();
            final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int status;
            while (true) {
                status = mediaCodec.dequeueOutputBuffer(bufferInfo, -1L);
                if (status >= 0) {
                    Logger.i("RecordingDevice", "Dequeued buffer " + bufferInfo.presentationTimeUs);
                    if ((MediaCodec.BUFFER_FLAG_CODEC_CONFIG & bufferInfo.flags) != 0) {
                        Logger.d("RecordingDevice", "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        bufferInfo.size = 0;
                    }

                    if (formatStatus == 0) { throw new RuntimeException("muxer hasn't started"); }

                    final ByteBuffer byteBuffer = outputBuffers[status];
                    bufferInfo.presentationTimeUs = ((System.nanoTime() - start) / 1000L);
                    mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
                    byteBuffer.clear();
                    mediaCodec.releaseOutputBuffer(status, false);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Logger.d(this, "end of stream reached");
                        break;
                    }
                } else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = mediaCodec.getOutputBuffers();
                } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (formatStatus != 0) { throw new RuntimeException("format changed twice"); }

                    final MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                    Logger.d("RecordingDevice", "encoder output format changed: %s", mediaFormat);
                    trackIndex = mediaMuxer.addTrack(mediaFormat);

                    formatStatus = 1;

                    if (withAudio) {
                        final RecordingDevice.AudioRecorder audioRec;
                        try {
                            audioRec = new RecordingDevice.AudioRecorder(this);
                        } catch (IOException io) {
                            Logger.e(this, "could not create audio recorder!");
                            return;
                        }
                        final Semaphore semaphore = new Semaphore(0);
                        final RecordingDevice.AudioMuxer audioMuxer =
                                new RecordingDevice.AudioMuxer(audioRec, mediaMuxer, semaphore);

                        final Thread audioRecorderThread = new Thread(audioRec, "AudioRecorder");
                        audioRecorderThread.start();

                        audioMuxerThread = new Thread(audioMuxer, "AudioMuxer");
                        audioMuxerThread.start();
                        semaphore.acquire();
                    } else {
                        mediaMuxer.start();
                    }

                    Logger.i("RecordingDevice", "Muxing");
                }
            }

            doneCoding = true;
            Logger.i("RecordingDevice", "Done recording");

            if (audioMuxerThread != null) {
                audioMuxerThread.join();
            }
            mediaMuxer.stop();

            final String[] scanPaths = new String[]{ mFile.getAbsolutePath() };
            MediaScannerConnection.scanFile(mContext, scanPaths, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(final String path, final Uri uri) {
                            Logger.i("RecordingDevice", "MediaScanner scanned recording %s", path);
                        }
                    });
        }
    }
}
