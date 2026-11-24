package uz.focus.examlock;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        Lock.enforce(context);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Imtihon qalqoni 22-dekabrgacha faol. O'chirib bo'lmaydi.";
    }
}
