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

package org.namelessrom.screencast.recording.audio;

import android.content.Context;
import android.media.AudioRecord;

import org.namelessrom.screencast.Logger;
import org.namelessrom.screencast.PreferenceHelper;

public abstract class BaseInputSource {
    private static final String TAG = "BaseInputSource";

    private static final int INPUT_MIC = 0;
    private static final int INPUT_DEV = 1;

    public static BaseInputSource getInputSource(final Context context) {
        final int id = PreferenceHelper.get(context)
                .getInt(PreferenceHelper.PREF_AUDIO_INPUT_DEVICE, INPUT_MIC);
        switch (id) {
            case INPUT_MIC: {
                Logger.v(TAG, "Using microphone as input source");
                return new MicInputsource();
            }
            case INPUT_DEV: {
                Logger.v(TAG, "Using remote submix as input source");
                return new SubmixInputsource();
            }
        }
        return new MicInputsource();
    }

    public abstract int getSource();

    public abstract int getChannel();

    public abstract int getChannelCount();

    public abstract int getEncoding();

    public abstract int getSampleRate();

    public abstract int getBitrate();

    public int getMinBufferSize() {
        return AudioRecord.getMinBufferSize(getSampleRate(), getChannel(), getEncoding()) * 10;
    }

    public AudioRecord createAudioRecord() {
        final int minBuf = getMinBufferSize();
        Logger.i(this, "getMinBufferSize -> %s", String.valueOf(minBuf));
        return new AudioRecord(getSource(), getSampleRate(), getChannel(), getEncoding(), minBuf);
    }

}
