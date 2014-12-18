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

    public static final String PREF_BITRATE   = "bitrate";
    public static final String PREF_FRAMERATE = "framerate";

    public static final String PREF_ENABLE_AUDIO = "enable_audio";
    public static final String PREF_ENABLE_NOISE_SUPPRESSION = "enable_noise_suppression";

    private static PreferenceHelper  sInstance;
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
        try {
            return Integer.parseInt(sPrefs.getString(name, String.valueOf(def)));
        } catch (NumberFormatException exc) {
            Logger.e(this, "getInt", exc);
            return def;
        }
    }

    public void setInt(final String name, final int value) {
        sPrefs.edit().putString(name, String.valueOf(value)).apply();
    }

    public boolean getBoolean(final String name, final boolean def) {
        return sPrefs.getBoolean(name, def);
    }
}
