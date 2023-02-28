package com.example.audioplayer_ver_dev02;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.ArrayList;
import java.util.List;

public class MediaBrowsingService extends MediaBrowserServiceCompat {
    // allows components and applications with a MediaBrowser to connect with their own media controller, and control the player
    // can provide an optional browsing API

    // =============================
    // MediaBrowserService Lifecycle
    // =============================
    // any android Service's behavior depends on whether it is *started* and/or *bound to one or more clients*
    // when created, being started, bounded, or both will keep the Service running
    // bound Services are destroyed when all clients disconnect
    // started Services must be explicitly stopped and destroyed
    //
    // if a MediaBrowserClient is only bound by a MediaBrowser (and not started), it is destroyed when the UI activity disconnects, so background play cannot work
    // startService() starts a service, Context.stopService() stops it externally, stopSelf() stops it internally
    //
    // the *media session* onPlay() callback should call startService()
    // the onStop() callback should call stopSelf()

    private static final String MEDIA_ROOT_ID = "media_root_id";
    private static final String EMPTY_ROOT_ID = "empty_root_id";

    private static final String LOG_TAG = "MyMediaBrowsingService";

    // MediaSession contains the Player, and provides a link between the app and the Player (kind of like a middle-man)
    private MediaSessionCompat mediaSession;
    // describes transport state (playing, paused, buffering, etc); the player's position; any accessible player actions; and an error code and optional error msg, when applicable
    private PlaybackStateCompat.Builder playerStateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();

        // create a MediaSession
        mediaSession = new MediaSessionCompat(MediaBrowsingService.this, LOG_TAG);

        // enable callbacks from MediaButtons and TransportControls
        // these flags are depreciated and enabled by default: https://developer.android.com/reference/kotlin/android/support/v4/media/session/MediaSessionCompat#FLAG_HANDLES_MEDIA_BUTTONS()
//        mediaSession.setFlags(
//                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS|
//                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
//        );

        // set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        playerStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE
                );
        mediaSession.setPlaybackState(playerStateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MyMediaSessionCallbacks());

        // set the session's token so that client activities can communicate with it
        setSessionToken(mediaSession.getSessionToken());
    }

    // controls access to the MediaBrowserService
    // returns content hierarchy's root node (BrowserRoot object); the content hierarchy can then be displayed in whole using onLoadChildren()
    //      can disable browsing by returning an empty hierarchy's root node
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // (optional) control access level for certain clients
        // allowBrowsing() would theoretically either operate on blacklist or whitelist. ex found in here: https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice#controlling_client_connections_with_ongetroot
//        if (allowBrowsing(clientPackageName, clientUid)) {
//            // returns a non-empty hierarchy's root ID
//            return new BrowserRoot(MEDIA_ROOT_ID, null);
//        } else {
//            // returns an empty hierarchy's root ID, disabling browsing
//            return new BrowserRoot(EMPTY_ROOT_ID, null);
//        }

        // (optional) alternatively, one can use logic to return a different content hierarchy depending on the connecting client's type
        // https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice#controlling_client_connections_with_ongetroot
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    // the client (program with a MediaBrowser) uses this to display a menu of the MediaBrowserService's content hierarchy/library
    // is called by the MediaBrowserCompat.subscribe() method, which sends back a List<MediaBrowserCompat.MediaItem>
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        //implementation example in the Universal Android Music Player app: https://github.com/android/uamp

        // browsing not allowed
//        if (TextUtils.equals(EMPTY_ROOT_ID, parentId)) {
//            result.sendResult(null);
//            return;
//        }

        // assume media catalog is loaded/cached

        List<MediaBrowserCompat.MediaItem> mediaItemList = new ArrayList<>();

        // check if this is the root ID
        if (MEDIA_ROOT_ID.equals(parentId)) {
            // build the MediaItem objects for the top level, and put them in mediaItemList
        } else {
            // examine the passed parentId to see which submenu we're at, and put the children of that menu in mediaItemList
        }
        result.sendResult(mediaItemList);
    }

    private AudioFocusRequest audioFocusRequest;
    private BecomingNoisyReceiver becomingNoisyReceiver = new BecomingNoisyReceiver();
    private class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // pause playback
            }
        }
    }
    private final String CHANNEL_ID = "39ccd723-8a52-474e-acf1-304f500415fb";
    int id = 1234567890;


    // MEDIA SESSION CALLBACKS
    private class MyMediaSessionCallbacks extends MediaSessionCompat.Callback {

        @SuppressLint("ServiceCast")
        @Override
        public void onPlay() {
            super.onPlay();
            // assign attributes to the audio focus request
            // in this case, AudioPlayer claims that it is playing music when requesting audio focus
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            // AudioPlayer requests AUDIOFOCUS_GAIN with the above attributes
            // other settings can dictate how AudioPlayer will behave regarding audio focus (check https://developer.android.com/reference/android/media/AudioFocusRequest.Builder)
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .build();
            //                                                                                        // if MediaBrowsingService.this doesn't work, try getApplicationContext()
            //                                                                                        // if not, search for "getting context in a service" (?)
            @SuppressLint("ServiceCast") AudioManager audioManager = (AudioManager) MediaBrowsingService.this.getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(audioFocusRequest);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // start the media browser service
                startService(new Intent(MediaBrowsingService.this, MediaBrowsingService.class));
                // activate the media session
                mediaSession.setActive(true);
                // start the player here
                // TODO: IMPLEMENT A PLAYER

                // register the BECOME_NOISY BroadcastReceiver, becomingNoisyReceiver, which pauses playback when user disconnects headphones, when applicable
                registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                MediaControllerCompat mediaControllerCompat = mediaSession.getController();
                MediaMetadataCompat mediaMetadataCompat = mediaControllerCompat.getMetadata();
                MediaDescriptionCompat mediaDescriptionCompat = mediaMetadataCompat.getDescription();

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MediaBrowsingService.this, CHANNEL_ID);

                notificationBuilder
                        // add currently playing track's metadata
                        .setContentTitle(mediaDescriptionCompat.getTitle())
                        .setContentText(mediaDescriptionCompat.getSubtitle())
                        .setSubText(mediaDescriptionCompat.getDescription())
                        .setLargeIcon(mediaDescriptionCompat.getIconBitmap())

                        // enable launching player by clicking notification
                        .setContentIntent(mediaControllerCompat.getSessionActivity())

                        // stop service when notification is swiped away
                        .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(MediaBrowsingService.this, PlaybackStateCompat.ACTION_STOP))

                        // make transport controls visible on lock screen
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                        // add app icon and set accent color
                        .setSmallIcon(R.drawable.ic_notification_icon_temp)
                        .setColor(Color.argb(255, 0, 50, 100))

                        // add pause button
                        .addAction(new NotificationCompat.Action(
                                R.drawable.ic_pause, getString(R.string.pause),
                                MediaButtonReceiver.buildMediaButtonPendingIntent(MediaBrowsingService.this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                        ))

                        // use MediaStyle features
                        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0)

                                //add cancel button
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(MediaBrowsingService.this, PlaybackStateCompat.ACTION_STOP))
                        );

                // display notification and place service in foreground
                MediaBrowsingService.this.startForeground(id, notificationBuilder.build());
            }
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onStop() {
            super.onStop();
        }
    }
}
