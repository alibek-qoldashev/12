package uz.focus.examlock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** Doimiy ishlaydigan servis: yangi ilova o'rnatilishi bilan tekshiradi. */
public class GuardService extends Service {
    private static final String CHANNEL = "guard";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver receiver;

    private final Runnable periodic = new Runnable() {
        @Override
        public void run() {
            if (Lock.isExpired()) {
                Lock.release(GuardService.this);
                return;
            }
            Lock.scanAll(GuardService.this);
            handler.postDelayed(this, 10 * 60 * 1000L);
        }
    };

    public static void start(Context c) {
        Lock.safe(() -> c.startForegroundService(new Intent(c, GuardService.class)));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Imtihon qalqoni", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Imtihon rejimi faol")
                .setContentText("O'yinlar 22-dekabrgacha bloklangan. Omad!")
                .setContentIntent(open)
                .setOngoing(true)
                .build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getData() != null) Lock.handlePackage(context, intent.getData().getSchemeSpecificPart());
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(receiver, f, Context.RECEIVER_EXPORTED);
        handler.post(periodic);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (receiver != null) unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
