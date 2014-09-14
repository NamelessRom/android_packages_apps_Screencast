package org.namelessrom.screencast;

import android.content.Context;

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

}
