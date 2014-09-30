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

package org.namelessrom.screencast.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.namelessrom.screencast.Logger;
import org.namelessrom.screencast.services.ScreencastService;

public class ControlReceiver extends BroadcastReceiver {

    @Override public void onReceive(final Context context, final Intent intent) {
        // just for information...
        Logger.i(this, "onReceive -> %s", intent.getAction());

        // let's take the intent we received, set the class and start the service
        intent.setClass(context, ScreencastService.class);
        context.startService(intent);
    }

}
