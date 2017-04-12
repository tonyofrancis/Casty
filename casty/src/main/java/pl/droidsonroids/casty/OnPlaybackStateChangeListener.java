package pl.droidsonroids.casty;

/**
 * Created by tonyofrancis on 4/12/17.
 */

public interface OnPlaybackStateChangeListener {
    void onPlaybackStateChanged(Casty casty);
    void onProgressChanged(long position,long duration);
}