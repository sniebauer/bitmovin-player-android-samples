package com.bitmovin.player.samples.playback.background;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.notification.BitmovinPlayerNotificationManager;
import com.bitmovin.player.notification.CustomActionReceiver;
import com.bitmovin.player.notification.DefaultMediaDescriptor;
import com.bitmovin.player.notification.NotificationListener;
import com.google.android.exoplayer2.util.NotificationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundPlaybackService extends Service
{
    private static final String NOTIFICATION_CHANNEL_ID = "com.bitmovin.player";
    private static final int NOTIFICATION_ID = 1;

    // Binder given to clients
    private final IBinder binder = new BackgroundBinder();
    private int bound = 0;

    private BitmovinPlayer player;
    private BitmovinPlayerNotificationManager playerNotificationManager;

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BackgroundBinder extends Binder
    {
        BitmovinPlayer getPlayer()
        {
            // Return this instance of BitmovinPlayer so clients can use the player instance
            return BackgroundPlaybackService.this.player;
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        final Context context = this;

        this.player = new BitmovinPlayer(context);

        // Create a BitmovinPlayerNotificationManager with the static create method
        // By passing null for the mediaDescriptionAdapter, a DefaultMediaDescriptionAdapter will be used internally.
        NotificationUtil.createNotificationChannel(context, NOTIFICATION_CHANNEL_ID, R.string.control_notification_channel, NotificationUtil.IMPORTANCE_LOW);
        this.playerNotificationManager = new BitmovinPlayerNotificationManager(
                this, NOTIFICATION_CHANNEL_ID, NOTIFICATION_ID, new DefaultMediaDescriptor(this.getAssets()), this.customActionReceiver);

        this.playerNotificationManager.setNotificationListener(new NotificationListener()
        {
            @Override
            public void onNotificationStarted(int notificationId, Notification notification)
            {
                startForeground(notificationId, notification);
            }

            @Override
            public void onNotificationCancelled(int notificationId)
            {
                stopSelf();
            }
        });

        // Attaching the BitmovinPlayer to the BitmovinPlayerNotificationManager
        this.playerNotificationManager.setPlayer(this.player);
    }

    @Override
    public void onDestroy()
    {
        this.playerNotificationManager.setPlayer(null);
        this.player.destroy();
        this.player = null;

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        this.bound++;
        return this.binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        this.bound--;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    private CustomActionReceiver customActionReceiver = new CustomActionReceiver()
    {
        @Override
        public Map<String, NotificationCompat.Action> createCustomActions(Context context)
        {
            return new HashMap<>();
        }

        @Override
        public List<String> getCustomActions(BitmovinPlayer player)
        {
            List<String> actions = new ArrayList<>();
            if (!player.isPlaying() && bound == 0)
            {
                actions.add(BitmovinPlayerNotificationManager.ACTION_STOP);
            }
            return actions;
        }

        @Override
        public void onCustomAction(BitmovinPlayer player, String action, Intent intent)
        {
            if (BitmovinPlayerNotificationManager.ACTION_STOP.equals(action))
            {
                stopSelf();
            }
        }
    };
}
