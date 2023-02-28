package com.example.audioplayer_ver_dev02;

import android.content.ComponentName;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MediaBrowsingClientActivity extends AppCompatActivity {
    // contains UI code, an associated MediaController, and a MediaBrowser

    // ======
    // FIELDS
    // ======
    private MediaBrowserCompat mediaBrowser;
    private ImageView playPause;

    // =====================================
    // CALLBACKS (LIFECYCLE CALLBACKS BELOW)
    // =====================================
    private MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();

            // get token from MediaBrowserServiceCompat/MediaBrowsingService.java for the MediaSession
            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

            // create a MediaControllerCompat instance with aforementioned token
            MediaControllerCompat mediaControllerCompat = new MediaControllerCompat(MediaBrowsingClientActivity.this, token);

            // save controller to media browser client/MediaBrowsingClientActivity.java
            MediaControllerCompat.setMediaController(MediaBrowsingClientActivity.this, mediaControllerCompat);

            // finish building UI
            buildTransportControls();
        }

        @Override
        public void onConnectionFailed() {
            // service/MediaBrowsingService refused connection
            super.onConnectionFailed();
        }

        @Override
        public void onConnectionSuspended() {
            // service/MediaBrowsingService crashed
            // disable transport controls until it automatically reconnects
            super.onConnectionSuspended();
        }
    };

    // enables callbacks from the media session when metadata or playback state is changed
    // registered to the media session in buildTransportControls(), and unregistered in onStop()
    MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
        }

        // if media session becomes invalid, media browser must be disconnected from MediaBrowserService/MediaBrowsingService.java
        // if mediaBrowser is not disconnected, user cannot view or control playback (might be *somewhat* not ideal, yk not being able to control the media playing through one's device)
        @Override
        public void onSessionDestroyed() {
            mediaBrowser.disconnect();
            // maybe schedule a reconnection using a *new* MediaBrowserCompat instance
            // check onCreate() below
        }
    };

    // =======
    // METHODS
    // =======
    // UI is fleshed out here
    // need to choose appropriate MediaControllerCompat.TransportControls method for each UI element (https://developer.android.com/reference/android/support/v4/media/session/MediaControllerCompat.TransportControls)
    // TransportControls methods send callbacks to media session in MediaBrowsingService
    // define a corresponding MediaSessionCompat.Callback for each media control (MyMediaSessionCallbacks)
    private void buildTransportControls() {
        // grab view for play/pause button
        playPause = findViewById(R.id.play_pause);

        // attach listener to play/pause button
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // play/pause is a toggle button, so you need to test state and choose action accordingly
                int playbackState = MediaControllerCompat.getMediaController(MediaBrowsingClientActivity.this).getPlaybackState().getState();
                if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(MediaBrowsingClientActivity.this).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(MediaBrowsingClientActivity.this).getTransportControls().play();
                }
            }
        });

        // declare and initialize MediaController
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MediaBrowsingClientActivity.this);

        // display initial metadata and playback state
        MediaMetadataCompat metadata = mediaController.getMetadata();
        PlaybackStateCompat playbackState = mediaController.getPlaybackState();

        // register callback to stay in sync
        mediaController.registerCallback(controllerCallback);
    }

    // ===================
    // LIFECYCLE CALLBACKS
    // ===================
    // constructs a MediaBrowserCompat
    // pass in your MediaBrowserServiceCompat and MediaBrowserCompat.ConnectionCallback that you've defined
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // create MediaBrowserServiceCompat
        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaBrowsingService.class),
                connectionCallbacks,
                null); // optional Bundle
    }

    // connects to MediaBrowserServiceCompat/MediaBrowsingService.java
    // if connection is successful, MediaBrowserCompat.ConnectionCallback:
    // creates media controller, links it to media session, links UI ctrls to MediaController, and registers controller to receive callbacks from media session
    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    // sets audio stream so app responds to device's volume ctrl
    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // see "stay in sync with the media session" https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowser-client#sync-with-mediasession
        if (MediaControllerCompat.getMediaController(MediaBrowsingClientActivity.this) != null) {
            MediaControllerCompat.getMediaController(MediaBrowsingClientActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }
}
