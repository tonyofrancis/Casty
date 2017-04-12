package pl.droidsonroids.casty;

/**
 * Created by tonyofrancis on 4/12/17.
 */

public interface OnConnectChangeListener {
    void onConnected(String castDeviceName);
    void onDisconnected(String castDeviceName);
    void onDiscovery(boolean castAvailable);
}