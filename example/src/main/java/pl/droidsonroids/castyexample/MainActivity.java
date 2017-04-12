package pl.droidsonroids.castyexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import pl.droidsonroids.casty.Casty;
import pl.droidsonroids.casty.MediaData;
import pl.droidsonroids.casty.OnConnectChangeListener;
import pl.droidsonroids.casty.OnPlaybackStateChangeListener;

public class MainActivity extends AppCompatActivity {
    private Button playButton;
    private Casty casty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        casty =  Casty.getInstanceWithMiniController(this);
        setContentView(R.layout.activity_main);
        setUpPlayButton();
        setUpMediaRouteButton();

        casty.addOnPlaybackStateChangeListener(new OnPlaybackStateChangeListener() {
            @Override
            public void onPlaybackStateChanged(Casty casty) {

                Log.d("casty","onPlaybackStateChanged");

                if(casty.isPaused()) {
                    playButton.setText("resume");
                }

                if(casty.isPlaying()) {
                    playButton.setText("pause");
                }
            }

            @Override
            public void onProgressChanged(long position, long duration) {
                Log.d("casty","onProgressChanged:"+position);
            }
        });


    }

    private void setUpPlayButton() {
        playButton = (Button) findViewById(R.id.button_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(casty.isIdle()) {
                    casty.play(createSampleMediaData());
                }else {
                    casty.togglePlayPause();
                }
            }
        });

        casty.addOnConnectChangeListener(onConnectChangeListener);
    }
    

    private OnConnectChangeListener onConnectChangeListener = new OnConnectChangeListener() {
        @Override
        public void onConnected(String castDeviceName) {
            playButton.setEnabled(true);
        }

        @Override
        public void onDisconnected(String castDeviceName) {
            playButton.setEnabled(false);
        }

        @Override
        public void onDiscovery(boolean castAvailable) {

            if(!castAvailable) {
                playButton.setVisibility(View.GONE);
            }else {
                playButton.setVisibility(View.VISIBLE);
            }
        }
    };

    private void setUpMediaRouteButton() {
        MediaRouteButton mediaRouteButton = (MediaRouteButton) findViewById(R.id.media_route_button);
        casty.attachMediaRouteButton(mediaRouteButton);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        casty.setMediaRouteMenuItem(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private static MediaData createSampleMediaData() {

        return new MediaData.Builder("http://distribution.bbb3d.renderfarming.net/video/mp4/bbb_sunflower_1080p_30fps_normal.mp4")
                .setStreamType(MediaData.STREAM_TYPE_BUFFERED)
                .setContentType("videos/mp4")
                .setMediaType(MediaData.MEDIA_TYPE_MOVIE)
                .setTitle("Sample title")
                .setSubtitle("Sample subtitle")
                .addPhotoUrl("https://peach.blender.org/wp-content/uploads/bbb-splash.png?x11217")
                .build();
    }
}
