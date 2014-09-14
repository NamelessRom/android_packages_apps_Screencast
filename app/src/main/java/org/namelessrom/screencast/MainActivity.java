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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.namelessrom.screencast.services.ScreencastService;

public class MainActivity extends Activity {

    private Button   mScreencastButton;
    private TextView mStatusText;

    @Override protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScreencastButton = ((Button) findViewById(R.id.button_screencast));
        mScreencastButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final boolean isRecording = Utils.isRecording(MainActivity.this);

                Utils.setRecording(MainActivity.this, !isRecording);

                final Intent i = new Intent();
                i.setAction(isRecording
                        ? ScreencastService.ACTION_STOP_SCREENCAST
                        : ScreencastService.ACTION_START_SCREENCAST);
                i.setClass(MainActivity.this, ScreencastService.class);
                startService(i);

                if (isRecording) {
                    refreshState();
                } else {
                    finish();
                }
            }
        });

        mStatusText = ((TextView) findViewById(R.id.statustext_screencast));
    }

    @Override public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        refreshState();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshState();
    }

    private void refreshState() {
        final boolean isRecording = Utils.isRecording(this);
        if (mScreencastButton != null) {
            mScreencastButton.setText(isRecording ? R.string.stop : R.string.start_screencast);
        }
        if (mStatusText != null) {
            mStatusText.setText(isRecording
                    ? getString(R.string.stop_description, getString(R.string.stop))
                    : getString(R.string.start_description, getString(R.string.start_screencast)));
        }
    }

}
