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

import android.media.AudioFormat;
import android.media.MediaRecorder;

public class MicInputsource extends BaseInputSource {
    @Override public int getSource() {
        return MediaRecorder.AudioSource.MIC;
    }

    @Override public int getChannel() {
        return AudioFormat.CHANNEL_IN_MONO;
    }

    @Override public int getChannelCount() {
        return 1;
    }

    @Override public int getEncoding() {
        return AudioFormat.ENCODING_PCM_16BIT;
    }

    @Override public int getSampleRate() {
        return 44100;
    }

    @Override public int getBitrate() {
        return 64 * 1024;
    }
}
