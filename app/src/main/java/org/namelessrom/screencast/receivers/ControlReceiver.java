package org.namelessrom.screencast.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.namelessrom.screencast.services.ScreencastService;

public class ControlReceiver extends BroadcastReceiver {

    @Override public void onReceive(final Context context, final Intent intent) {
        // let's take the intent we received, set the class and start the service
        intent.setClass(context, ScreencastService.class);
        context.startService(intent);
    }

}
