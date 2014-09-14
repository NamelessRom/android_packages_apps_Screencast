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

import org.namelessrom.screencast.services.ScreencastService;

public class MainActivity extends Activity {

    private Button mStartScreencastButton;
    private Button mStopScreencastButton;

    @Override protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartScreencastButton = ((Button) findViewById(R.id.start_screencast));
        mStartScreencastButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Utils.setRecording(MainActivity.this, true);

                final Intent i = new Intent(ScreencastService.ACTION_START_SCREENCAST);
                i.setClass(MainActivity.this, ScreencastService.class);
                startService(i);

                finish();
            }
        });

        mStopScreencastButton = ((Button) findViewById(R.id.stop_screencast));
        mStopScreencastButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Utils.setRecording(MainActivity.this, false);

                final Intent i = new Intent(ScreencastService.ACTION_STOP_SCREENCAST);
                i.setClass(MainActivity.this, ScreencastService.class);
                startService(i);

                refreshState();
            }
        });
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
        if (mStartScreencastButton != null) {
            mStartScreencastButton.setEnabled(!isRecording);
        }
        if (mStopScreencastButton != null) {
            mStopScreencastButton.setEnabled(isRecording);
        }
    }

}
