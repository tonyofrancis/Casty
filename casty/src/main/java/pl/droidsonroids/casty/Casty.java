package pl.droidsonroids.casty;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.MediaRouteButton;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.AppVisibilityListener;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity;

import java.util.HashSet;

/**
 * Core class of Casty. It manages buttons/widgets and gives access to the media player.
 */
public class Casty {

    static CastOptions customCastOptions;

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isValid;
    private Activity activity;
    private CastContext castContext;
    private CastSession castSession;
    private SessionManager sessionManager;
    private RemoteMediaClient remoteMediaClient;
    private HashSet<OnConnectChangeListener> onConnectChangeListeners = new HashSet<>();
    private HashSet<OnPlaybackStateChangeListener> onPlaybackStateChangeListeners = new HashSet<>();
    private Class<? extends ExpandedControllerActivity> expandedControllerActivity = ExpandedControlsActivity.class;

    /**
     * Sets the custom receiver ID. Should be used in the {@link Application} class.
     *
     * @param receiverId the custom receiver ID, e.g. Styled Media Receiver - with custom logo and background
     */
    public static void init(@NonNull String receiverId) {

        if(receiverId == null) {
            throw new NullPointerException("ReceiverId cannot be null");
        }

        init(Utils.getDefaultCastOptions(receiverId));
    }

    /**
     * Sets the custom CastOptions, should be used in the {@link Application} class.
     *
     * @param castOptions the custom CastOptions object, must include a receiver ID
     */
    public static void init(@NonNull CastOptions castOptions) {

        if(castOptions == null) {
            throw new NullPointerException("CastOptions cannot be null");
        }

        Casty.customCastOptions = castOptions;
    }

    /**
     * Gets new Casty instance.
     *
     * @param activity {@link Activity} in which Casty object is created
     * @return the Casty object.
     */
    public static Casty getInstance(@NonNull Activity activity) {

        if(activity == null) {
            throw new NullPointerException("Activity cannot be null");
        }

        boolean isValid = Utils.isPlayServicesAvailable(activity.getApplicationContext());

        return new Casty(activity,isValid);
    }

    /**
     * Gets new Casty instance with a MiniController attached to the activity.
     *
     * @param activity {@link Activity} in which Casty object is created
     * @return the Casty object or null if Google Play Services is not on the device.
     */
    public static Casty getInstanceWithMiniController(@NonNull Activity activity) {

        Casty casty = Casty.getInstance(activity);

        if(casty.isValid()) {
            Casty.addMiniController(activity);
        }

        return casty;
    }

    private static void addMiniController(final Activity activity) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {

                if(activity.findViewById(R.id.casty_mini_controller) == null) {

                    ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
                    View rootView = contentView.getChildAt(0);

                    if(rootView != null) {

                        FrameLayout frameLayout = new FrameLayout(activity);
                        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT));

                        contentView.removeView(rootView);

                        ViewGroup.LayoutParams oldRootParams = rootView.getLayoutParams();
                        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(oldRootParams.width,oldRootParams.height);
                        rootView.setLayoutParams(rootParams);

                        frameLayout.addView(rootView);

                        activity.getLayoutInflater().inflate(R.layout.mini_controller,frameLayout,true);
                        activity.setContentView(frameLayout);
                    }
                }

            }
        });
    }

    private Casty(Activity activity,boolean isValid) {

        this.isValid = isValid;

        if(isValid) {
            this.activity = activity;
            this.castContext = CastContext.getSharedInstance(activity.getApplicationContext());
            this.sessionManager = this.castContext.getSessionManager();
            this.activity.getApplication().registerActivityLifecycleCallbacks(createActivityCallbacks());
        }
    }

    private Application.ActivityLifecycleCallbacks createActivityCallbacks() {
        return new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

                if(Casty.this.activity == activity) {
                    registerCastStateListener();
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                //no-op
            }

            @Override
            public void onActivityResumed(Activity activity) {

                if (Casty.this.activity == activity) {
                    updateCastSession();
                    registerCastStateListener();
                    registerRemoteClientListener();
                    registerProgressListener();
                    registerSessionManagerListener();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {

                if (Casty.this.activity == activity) {
                    unregisterCastStateListener();
                    unregisterRemoteClientListener();
                    unregisterProgressListener();
                    unregisterSessionManagerListener();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                //no-op
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                //no-op
            }

            @Override
            public void onActivityDestroyed(Activity activity) {

                if (Casty.this.activity == activity) {
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                    onConnectChangeListeners.clear();
                    onPlaybackStateChangeListeners.clear();
                }
            }
        };
    }

    private final SessionManagerListener<CastSession> sessionManagerListener =  new SessionManagerListener<CastSession>() {
        @Override
        public void onSessionStarted(CastSession castSession, String s) {

            activity.invalidateOptionsMenu();
            onConnected(castSession);
        }

        @Override
        public void onSessionEnded(CastSession castSession, int i) {

            activity.invalidateOptionsMenu();
            onDisconnected(castSession);
        }

        @Override
        public void onSessionResumed(CastSession castSession, boolean b) {

            activity.invalidateOptionsMenu();
            onConnected(castSession);
        }

        @Override
        public void onSessionStarting(CastSession castSession) {
            //no-op
        }

        @Override
        public void onSessionStartFailed(CastSession castSession, int i) {
            //no-op
        }

        @Override
        public void onSessionEnding(CastSession castSession) {
            //no-op
        }

        @Override
        public void onSessionResuming(CastSession castSession, String s) {
            //no-op
        }

        @Override
        public void onSessionResumeFailed(CastSession castSession, int i) {
            //no-op
        }

        @Override
        public void onSessionSuspended(CastSession castSession, int i) {
            //no-op
        }
    };

    private void registerSessionManagerListener() {
        sessionManager.addSessionManagerListener(sessionManagerListener,CastSession.class);
    }

    private void unregisterSessionManagerListener() {
        sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    private void registerRemoteClientListener() {

        if(isRemoteClientLoaded()) {
            remoteMediaClient.addListener(remoteClientListener);
        }
    }

    private void unregisterRemoteClientListener() {

        if(isRemoteClientLoaded()) {
            remoteMediaClient.removeListener(remoteClientListener);
        }
    }

    private void registerCastStateListener() {
        castContext.addCastStateListener(castStateListener);
    }

    private void unregisterCastStateListener() {
        castContext.removeCastStateListener(castStateListener);
    }

    private void registerProgressListener() {

        if(isRemoteClientLoaded()) {
            remoteMediaClient.addProgressListener(progressListener,1000);
        }
    }

    private void unregisterProgressListener() {

        if(isRemoteClientLoaded()) {
            remoteMediaClient.removeProgressListener(progressListener);
        }
    }

    /**
     * Sets the discovery menu item on a toolbar.
     * Should be used in {@link Activity#onCreateOptionsMenu(Menu)}.
     *
     * @param menu Menu in which MenuItem should be added
     */
    public void setMediaRouteMenuItem(@NonNull final Menu menu) {

        if(menu == null) {
            throw new NullPointerException("Menu cannot be null");
        }

        if(isValid()) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.getMenuInflater().inflate(R.menu.casty_discovery, menu);
                    CastButtonFactory.setUpMediaRouteButton(activity, menu, R.id.casty_media_route_menu_item);
                }
            });
        }
    }

    /**
     * Makes {@link MediaRouteButton} react to discovery events.
     *
     * @param mediaRouteButton Button to be set up
     * @return Casty instance
     */
    public Casty  attachMediaRouteButton(@NonNull final MediaRouteButton mediaRouteButton) {

        if(mediaRouteButton == null) {
            throw new NullPointerException("MediaRouteButton cannot be null");
        }

        if(isValid()) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    CastButtonFactory.setUpMediaRouteButton(activity, mediaRouteButton);
                }
            });
        }

        return this;
    }

    /**
     * Sets the ExpandedControllerActivity.
     *
     * @param expandedControllerActivity the ExpandedControllerActivity
     * @return Casty instance
     * */
    public Casty setExpandedControllerActivity(@Nullable Class<? extends ExpandedControllerActivity> expandedControllerActivity) {

        if(isValid()) {
            this.expandedControllerActivity = expandedControllerActivity;
        }

        return this;
    }

    /**
     * Checks if a Google Cast device is connected.
     *
     * @return true if a Google Cast is connected, false otherwise
     */
    public boolean isConnected() {
        return castSession != null && castSession.isConnected();
    }

    /**
     * Play content through Google Cast.
     *
     * @param mediaData bundled options used to load the content on Google Cast
     * */
    public void play(@NonNull MediaData mediaData) {

        if(mediaData == null) {
            throw new NullPointerException("MediaData cannot be null");
        }

        if(isValid() && isRemoteClientLoaded()) {

            remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
                @Override
                public void onStatusUpdated() {

                    if(expandedControllerActivity != null){
                        Utils.startExpandedControlsActivity(activity,expandedControllerActivity);
                    }

                    remoteMediaClient.removeListener(this);
                }

                @Override
                public void onMetadataUpdated() {
                }

                @Override
                public void onQueueStatusUpdated() {
                }

                @Override
                public void onPreloadStatusUpdated() {
                }

                @Override
                public void onSendingRemoteMediaRequest() {
                }

                @Override
                public void onAdBreakStatusUpdated() {
                }
            });

          remoteMediaClient.load(mediaData.createMediaInfo(),mediaData.autoPlay,mediaData.position);
        }
    }

    /**
     * Seek to a specified position for the currently casted content.
     *
     * @param position seek position
     * */
    public void seek(long position) {

        if(isValid() && isRemoteClientLoaded()) {
            remoteMediaClient.seek(position);
        }
    }

    /**
     * Stop playback for the currently casted content.
     * */
    public void stop() {

        if(isValid() && isRemoteClientLoaded()) {
            remoteMediaClient.stop();
        }
    }

    /**
     * Pause playback for the currently casted content.
     * */
    public void pause() {

        if(isValid() && isRemoteClientLoaded()) {
            remoteMediaClient.pause();
        }
    }

    /**
     * Toggle playback between play and pause for the currently casted content.
     * */
    public void togglePlayPause() {

        if(isValid() && isRemoteClientLoaded()) {
            remoteMediaClient.togglePlayback();
        }
    }

    /**
     * Checks if the Google Cast device is connected and playing content.
     *
     * @return true if the Google Cast device is connected and playing content
     * */
    public boolean isPlaying() {

        return isValid() && isRemoteClientLoaded() && remoteMediaClient.isPlaying();
    }

    /**
     * Checks if the Google Cast device is buffering the loaded content.
     *
     * @return true if the Google Cast device is buffering the loaded content
     * */
    public boolean isBuffering() {

        return isValid() && isRemoteClientLoaded() && remoteMediaClient.isBuffering();
    }

    /**
     * Checks if the Google Cast device has paused the loaded content.
     *
     * @return true if the Google Cast device has paused the loaded content
     * */
    public boolean isPaused() {
        return isValid() && isRemoteClientLoaded() && remoteMediaClient.isPaused();
    }

    /**
     * Checks if the Google Cast device is in an Idle State
     *
     * @return true if the Google Cast device is in an Idle state
     * */
    public boolean isIdle() {

        return isValid() && isRemoteClientLoaded()
                && remoteMediaClient.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE;
    }

    /**
     * Checks if the Google Cast device is currently playing live content.
     *
     * @return true if the Google Cast device is currently playing live content
     * */
    public boolean isLiveStream() {

        return isValid() && isRemoteClientLoaded() && remoteMediaClient.isLiveStream();
    }

    /**
     * Checks if this instance of Casty is valid for use.
     *
     * @return true of the Casty instance if valid for use
     * */
    public boolean isValid() {
        return isValid;
    }

    private boolean isRemoteClientLoaded() {
        return remoteMediaClient != null;
    }

    /**
     * Attaches an {@link OnConnectChangeListener} to Casty
     *
     * @param onConnectChangeListener Connect change callback
     * @return Casty instance
     */
    public Casty addOnConnectChangeListener(@NonNull OnConnectChangeListener onConnectChangeListener) {

        if(onConnectChangeListener == null) {
            throw new NullPointerException("OnConnectChangeListener is null");
        }

        if(isValid()) {

            if(onConnectChangeListeners.contains(onConnectChangeListener)) {
                return this;
            }

            onConnectChangeListeners.add(onConnectChangeListener);
        }

        return this;
    }

    /**
     * Detaches an {@link OnConnectChangeListener} from Casty
     *
     * @param onConnectChangeListener Connect change callback
     * @return Casty instance
     */
    public Casty removeOnConnectChangeListener(@NonNull OnConnectChangeListener onConnectChangeListener) {

        if(isValid() && onConnectChangeListener != null) {
            onConnectChangeListeners.remove(onConnectChangeListener);
        }

        return this;
    }

    /**
     * Attaches an {@link OnPlaybackStateChangeListener} to Casty
     *
     * @param onPlaybackStateChangeListener Playback state change listener
     * @return Casty instance
     */
    public Casty addOnPlaybackStateChangeListener(@NonNull OnPlaybackStateChangeListener onPlaybackStateChangeListener) {

        if(onPlaybackStateChangeListener == null) {
            throw new NullPointerException("onPlaybackStateChangeListener is null");
        }

        if(isValid()) {

            if(onPlaybackStateChangeListeners.contains(onPlaybackStateChangeListener)) {
                return this;
            }

            onPlaybackStateChangeListeners.add(onPlaybackStateChangeListener);
        }

        return this;
    }

    /**
     * Detaches an {@link OnPlaybackStateChangeListener} from Casty
     *
     * @param onPlaybackStateChangeListener playback state change listener
     * @return Casty instance
     */
    public Casty removeOnPlaybackStateChangeListener(@NonNull OnPlaybackStateChangeListener onPlaybackStateChangeListener) {

        if(isValid() && onPlaybackStateChangeListener != null) {
            onPlaybackStateChangeListeners.remove(onPlaybackStateChangeListener);
        }

        return this;
    }

    private void onConnected(CastSession castSession) {
        this.castSession = castSession;

        unregisterRemoteClientListener();
        unregisterProgressListener();
        this.remoteMediaClient = castSession.getRemoteMediaClient();
        registerRemoteClientListener();
        registerProgressListener();

        String castDeviceName = Utils.getCastDeviceName(castSession);

        for (OnConnectChangeListener onConnectChangeListener : onConnectChangeListeners) {
            onConnectChangeListener.onConnected(castDeviceName);
        }
    }

    private void onDisconnected(CastSession castSession) {
        this.castSession = null;

        unregisterRemoteClientListener();
        unregisterProgressListener();
        this.remoteMediaClient = null;

        String castDeviceName = Utils.getCastDeviceName(castSession);

        for (OnConnectChangeListener onConnectChangeListener : onConnectChangeListeners) {
            onConnectChangeListener.onDisconnected(castDeviceName);
        }
    }

    private final CastStateListener castStateListener = new CastStateListener() {
        @Override
        public void onCastStateChanged(int state) {

            boolean castAvailable =  state != CastState.NO_DEVICES_AVAILABLE && isValid();

            for (OnConnectChangeListener onConnectChangeListener : onConnectChangeListeners) {
                onConnectChangeListener.onDiscovery(castAvailable);
            }
        }
    };

    private final RemoteMediaClient.Listener remoteClientListener = new RemoteMediaClient.Listener() {
        @Override
        public void onStatusUpdated() {

            for (OnPlaybackStateChangeListener onPlaybackStateChangeListener : onPlaybackStateChangeListeners) {
                onPlaybackStateChangeListener.onPlaybackStateChanged(Casty.this);
            }
        }

        @Override
        public void onMetadataUpdated() {
        }

        @Override
        public void onQueueStatusUpdated() {
        }

        @Override
        public void onPreloadStatusUpdated() {
        }

        @Override
        public void onSendingRemoteMediaRequest() {
        }

        @Override
        public void onAdBreakStatusUpdated() {
        }
    };

    private final RemoteMediaClient.ProgressListener progressListener = new RemoteMediaClient.ProgressListener() {
        @Override
        public void onProgressUpdated(long progress, long duration) {

            for (OnPlaybackStateChangeListener onPlaybackStateChangeListener : onPlaybackStateChangeListeners) {
                onPlaybackStateChangeListener.onProgressChanged(progress,duration);
            }
        }
    };

    private void updateCastSession() {

        CastSession newCastSession = sessionManager.getCurrentCastSession();

        if(castSession != newCastSession) {
            castSession = newCastSession;
        }

        if(castSession != null && castSession.isConnected()) {
            onConnected(castSession);
        }else {
            onDisconnected(newCastSession);
        }
    }
}