package pl.droidsonroids.casty;

import android.content.Context;
import android.support.v7.media.MediaControlIntent;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import java.util.List;

public class CastOptionsProvider implements OptionsProvider {
    @Override
    public CastOptions getCastOptions(Context context) {

        if(Casty.customCastOptions == null) {
            Casty.customCastOptions = Utils.getDefaultCastOptions(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID);
        }

        return Casty.customCastOptions;
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
