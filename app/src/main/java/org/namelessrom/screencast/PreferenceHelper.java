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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceHelper {
    public static final String PREF_BITRATE = "bitrate";
    public static final String PREF_FRAMERATE = "framerate";

    public static final String PREF_ENABLE_AUDIO = "enable_audio";
    public static final String PREF_ENABLE_NOISE_SUPPRESSION = "enable_noise_suppression";

    public static final String PREF_AUDIO_INPUT_DEVICE = "audio_input_device";

    private static PreferenceHelper sInstance;
    private static SharedPreferences sPrefs;

    private PreferenceHelper(final Context context) {
        sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static PreferenceHelper get(final Context context) {
        if (sInstance == null) {
            sInstance = new PreferenceHelper(context);
        }
        return sInstance;
    }

    public int getInt(final String name, final int def) {
        return Utils.parseInt(getString(name, String.valueOf(def)));
    }

    public String getString(final String name, final String value) {
        return sPrefs.getString(name, value);
    }

    public void setString(final String name, final String value) {
        sPrefs.edit().putString(name, value).apply();
    }

    public void setInt(final String name, final int value) {
        setString(name, String.valueOf(value));
    }

    public boolean getBoolean(final String name, final boolean def) {
        return sPrefs.getBoolean(name, def);
    }
}
