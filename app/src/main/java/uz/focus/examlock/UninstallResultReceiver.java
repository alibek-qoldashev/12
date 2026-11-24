package uz.focus.examlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

/** O'chirish natijasi. Muvaffaqiyatsiz bo'lsa ham o'yin "suspended" holatda qoladi va ochilmaydi. */
public class UninstallResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        Log.i("ExamLock", "Uninstall " + intent.getStringExtra("pkg") + " status=" + status
                + " " + intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
    }
}
