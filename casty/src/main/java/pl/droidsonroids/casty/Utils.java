package pl.droidsonroids.casty;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tonyofrancis on 4/11/17.
 */

final class Utils {

    static boolean isPlayServicesAvailable(Context context) {

        return GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    static CastOptions getDefaultCastOptions(String receiverId) {

        List<String> buttonActions = Arrays.asList(
                MediaIntentReceiver.ACTION_REWIND,
                MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                MediaIntentReceiver.ACTION_FORWARD,
                MediaIntentReceiver.ACTION_STOP_CASTING);


        int[] compatButtonAction = { 1, 3 };

        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setActions(buttonActions, compatButtonAction)
                .setTargetActivityClassName(ExpandedControlsActivity.class.getName())
                .build();

        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(ExpandedControlsActivity.class.getName())
                .build();


        return new CastOptions.Builder()
                .setReceiverApplicationId(receiverId)
                .setCastMediaOptions(mediaOptions)
                .build();
    }

    static void startExpandedControlsActivity(Activity activity, Class<? extends ExpandedControllerActivity> expandedControllerActivityClass) {

        activity.startActivity(new Intent(activity, expandedControllerActivityClass));
    }

    static String getCastDeviceName(CastSession castSession) {

        String castDeviceName = "Unknown";

        if(castSession != null && castSession.getCastDevice() != null) {
            castDeviceName = castSession.getCastDevice().getFriendlyName();
        }

        return castDeviceName;
    }
}
