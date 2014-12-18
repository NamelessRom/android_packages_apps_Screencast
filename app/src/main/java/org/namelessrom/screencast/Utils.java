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

package org.namelessrom.screencast;

import android.content.Context;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.text.TextUtils;

import org.namelessrom.screencast.recording.VideoEncoderCap;
import org.xml.sax.Attributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;

import safesax.Parsers;

public class Utils {

    public static void setRecording(final Context context, final boolean isRecording) {
        context.getSharedPreferences("preferences", 0)
                .edit()
                .putBoolean("recording", isRecording)
                .apply();
    }

    public static boolean isRecording(final Context context) {
        return context.getSharedPreferences("preferences", 0).getBoolean("recording", false);
    }

    public static ArrayList<VideoEncoderCap> getVideoEncoderCaps() {
        final ArrayList<VideoEncoderCap> videoEncoderCaps = new ArrayList<>();

        try {
            final File mediaProfiles = new File("/system/etc/media_profiles.xml");
            final FileInputStream fis = new FileInputStream(mediaProfiles);
            byte[] byteArray = new byte[(int) mediaProfiles.length()];
            Logger.v("getVideoEncoderCaps", "fis.read(): %s",
                    fis.read(byteArray, 0, byteArray.length));
            final String str = new String(byteArray);
            final RootElement rootElement = new RootElement("MediaSettings");
            final Element videoElement = rootElement.requireChild("VideoEncoderCap");
            final ElementListener elementListener = new ElementListener() {
                public void end() { }

                public void start(Attributes attributes) {
                    if (TextUtils.equals(attributes.getValue("name"), "h264")) {
                        final VideoEncoderCap videoEncoderCap = new VideoEncoderCap(attributes);
                        Logger.v("getVideoEncoderCaps()", videoEncoderCap.toString());
                        videoEncoderCaps.add(videoEncoderCap);
                    }
                }
            };
            videoElement.setElementListener(elementListener);

            final StringReader stringReader = new StringReader(str);
            Parsers.parse(stringReader, rootElement.getContentHandler());
        } catch (Exception e) {
            Logger.e("getVideoEncoderCaps", "Uhoh", e);
        }

        return videoEncoderCaps;
    }

    public static String toBitrate(final int bitrate) {
        final String value = String.valueOf(bitrate);
        String tmp;

        if (value.length() > 6) {
            tmp = value.substring(value.length() - 6, value.length());
            //Logger.i("toBitrate #1", tmp);
            if (TextUtils.equals("000000", tmp)) {
                return value.substring(0, value.length() - 6) + " mbit/s";
            }
        }

        if (value.length() > 3) {
            tmp = value.substring(value.length() - 3, value.length());
            //Logger.i("toBitrate #2", tmp);
            if (TextUtils.equals("000", tmp)) {
                return value.substring(0, value.length() - 3) + " kbit/s";
            }
        }

        return value;
    }
}
