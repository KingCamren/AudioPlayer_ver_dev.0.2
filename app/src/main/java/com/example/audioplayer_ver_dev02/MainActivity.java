package com.example.audioplayer_ver_dev02;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

// the preferred audio app architecture is client/server
// with a MediaBrowserService server and a MediaBrowser Activity client
// this allows multiple apps to access the track, such as Wear OS or Google Home
// MediaBrowserService also provides a (optional) browsing API, which can display playlists, media libraries, etc

// SERVICE VS ACTIVITY
// Activities represent the app's UI, and run primarily when the app is open in the foreground
// Services run when the app is in the background, or even swiped out entirely in the recent apps
// Services run background tasks such as data management, background calculations, and (importantly here) background playback, among other things

public class MainActivity extends AppCompatActivity {

    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private boolean playWhenReady;
    private int currentItem;
    private long playbackPosition;
    private final Player.Listener playbackStateListener = new PlaybackStateListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playWhenReady = true;
        currentItem = 0;
        playbackPosition = 0L;
    }

    // api 24 introduced multiple window support, allowing app to be visible while inactive, so player initialization must happen in onStart
    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    // api 23 and lower requires program to wait as long as possible before grabbing resources, done by waiting until onResume to call initializePlayer()
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT <= 23 || exoPlayer == null)) {
            initializePlayer();
        }
    }

    // before api 23, onStop not always called, so player release must be done asap w/ onPause
    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    // api 24 brought multi- and split-window mode, so onStop is always called
    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.player_view);
        playerView.setPlayer(exoPlayer);
        MediaItem mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3));
        exoPlayer.setMediaItem(mediaItem);

        // for resuming play after pausing program
        exoPlayer.setPlayWhenReady(playWhenReady);
        exoPlayer.seekTo(currentItem, playbackPosition);
        exoPlayer.addListener(playbackStateListener);
        exoPlayer.prepare();
    }

    private void releasePlayer() {
        playbackPosition = exoPlayer.getCurrentPosition();
        currentItem = exoPlayer.getCurrentMediaItemIndex();
        playWhenReady = exoPlayer.getPlayWhenReady();
        exoPlayer.removeListener(playbackStateListener);
        exoPlayer.release();
        exoPlayer = null;
    }

    private void hideSystemUi() {
        WindowInsetsControllerCompat windowInsetsControllerCompat = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsControllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }
}

class PlaybackStateListener implements Player.Listener {

    private final String TAG = "AudioPlayer_dev.0.2";
    String state;

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            // has been instantiated, but ExoPlayer.prepare() hasn't been called
            case ExoPlayer.STATE_IDLE:
                state = "ExoPlayer.STATE_IDLE      -";
                break;
            // player's data buffer has run out, and must load more data
            case ExoPlayer.STATE_BUFFERING:
                state = "ExoPlayer.STATE_BUFFERING -";
                break;
            // media being played has finished
            case ExoPlayer.STATE_ENDED:
                state = "ExoPlayer.STATE_ENDED     -";
                break;
            // ready to play from current position, and will play automatically if playWhenReady = true
            case ExoPlayer.STATE_READY:
                state = "ExoPlayer.STATE_READY     -";
                break;
            default:
                state = "UNKNOWN_STATE             -";
                break;
        }
        Log.d(TAG, "changed state to " + state);
    }
}

