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

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.namelessrom.screencast.recording.VideoEncoderCap;

import java.util.ArrayList;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    // handled via xml
    //private CheckBoxPreference mShowTouches;

    private ListPreference mBitrate;
    private ListPreference mFramerate;

    @Override public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_screencast);

        mBitrate = (ListPreference) findPreference(PreferenceHelper.PREF_BITRATE);
        mBitrate.setEnabled(false);

        mFramerate = (ListPreference) findPreference(PreferenceHelper.PREF_FRAMERATE);
        mFramerate.setEnabled(false);

        // fill our preferences with values
        new ReadValuesTask().execute();
    }

    @Override public boolean onPreferenceChange(Preference preference, Object o) {
        if (mBitrate == preference) {
            final String tmp = String.valueOf(o);
            final int value;
            try {
                value = Integer.parseInt(tmp);
                PreferenceHelper.get(getActivity()).setInt(PreferenceHelper.PREF_BITRATE, value);
                mBitrate.setSummary(Utils.toBitrate(value));
            } catch (NumberFormatException exception) {
                return false;
            }
            return true;
        } else if (mFramerate == preference) {
            final String tmp = String.valueOf(o);
            final int value;
            try {
                value = Integer.parseInt(tmp);
                PreferenceHelper.get(getActivity()).setInt(PreferenceHelper.PREF_FRAMERATE, value);
                mFramerate.setSummary(String.format("%s fps", value));
            } catch (NumberFormatException exception) {
                return false;
            }
            return true;
        }

        return false;
    }

    private class ReadValuesTask extends AsyncTask<Void, Void, VideoEncoderCap> {

        @Override protected VideoEncoderCap doInBackground(final Void... voids) {
            final ArrayList<VideoEncoderCap> caps = Utils.getVideoEncoderCaps();
            return caps != null ? caps.get(0) : null;
        }

        @Override protected void onPostExecute(final VideoEncoderCap videoEncoderCap) {
            if (videoEncoderCap == null) {
                return;
            }

            final ArrayList<String> entries = new ArrayList<String>();
            final ArrayList<String> values = new ArrayList<String>();
            int tmp;

            if (mBitrate != null) {
                // collect bitrates
                for (final int i : VideoEncoderCap.VIDEO_BITRATES) {
                    if (i > videoEncoderCap.maxBitRate) {
                        // get out of here!
                        break;
                    } else if (i < videoEncoderCap.minBitRate) {
                        // lets get the higher ones!
                        continue;
                    }
                    entries.add(Utils.toBitrate(i));
                    values.add(String.valueOf(i));
                }

                // set the collected bitrates as entries and entry values
                mBitrate.setEntries(entries.toArray(new String[entries.size()]));
                mBitrate.setEntryValues(values.toArray(new String[values.size()]));

                // get the current bitrate
                tmp = PreferenceHelper.get(getActivity())
                        .getInt(PreferenceHelper.PREF_BITRATE, videoEncoderCap.maxBitRate);
                // cap it
                if (tmp > VideoEncoderCap.MAX_VIDEO_BITRATE) {
                    tmp = VideoEncoderCap.MAX_VIDEO_BITRATE;
                }

                // set it up
                mBitrate.setValue(String.valueOf(tmp));
                mBitrate.setSummary(Utils.toBitrate(tmp));
                mBitrate.setOnPreferenceChangeListener(SettingsFragment.this);

                // enable it
                mBitrate.setEnabled(true);
            }

            if (mFramerate != null) {
                entries.clear();
                values.clear();
                // collect framerates
                for (final int i : VideoEncoderCap.VIDEO_FRAMERATES) {
                    if (i > videoEncoderCap.maxFrameRate) {
                        // get out of here!
                        break;
                    } else if (i < videoEncoderCap.minFrameRate) {
                        // lets get the higher ones!
                        continue;
                    }
                    entries.add(String.format("%s fps", i));
                    values.add(String.valueOf(i));
                }

                // set the collected framerates as entries and entry values
                mFramerate.setEntries(entries.toArray(new String[entries.size()]));
                mFramerate.setEntryValues(values.toArray(new String[values.size()]));

                // get the current framerate
                tmp = PreferenceHelper.get(getActivity())
                        .getInt(PreferenceHelper.PREF_FRAMERATE, videoEncoderCap.maxFrameRate);
                // cap it
                if (tmp > VideoEncoderCap.MAX_VIDEO_FRAMERATE) {
                    tmp = VideoEncoderCap.MAX_VIDEO_FRAMERATE;
                }

                // set it up
                mFramerate.setValue(String.valueOf(tmp));
                mFramerate.setSummary(String.format("%s fps", tmp));
                mFramerate.setOnPreferenceChangeListener(SettingsFragment.this);

                // enable it
                mFramerate.setEnabled(true);
            }
        }

    }

}
