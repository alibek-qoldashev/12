package uz.focus.examlock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, p * 2, p, p);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Lock.enforce(this);
        render();
    }

    private void render() {
        root.removeAllViews();
        title("🛡 Imtihon Qalqoni");

        if (!Lock.isOwner(this)) {
            if (Lock.prefs(this).getBoolean("released", false)) {
                text("✅ Qulf tugadi. Imtihoningiz qanday o'tdi? Endi bu ilovani o'chirishingiz mumkin.");
                button("Ilovani o'chirish", () -> startActivity(
                        new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getPackageName()))));
            } else {
                text("⚠️ Ilova hali faollashtirilmagan.\n\nKompyuterdan ADB orqali quyidagi buyruqni bajaring:");
                mono("adb shell dpm set-device-owner " + getPackageName() + "/.AdminReceiver");
                text("Batafsil yo'riqnoma README faylida.");
            }
            return;
        }

        long left = Lock.END_MILLIS - System.currentTimeMillis();
        long days = left / 86_400_000L, hours = (left / 3_600_000L) % 24, mins = (left / 60_000L) % 60;
        big(days + " kun " + hours + " soat " + mins + " daqiqa");
        text("qulf tugashigacha (" + DateFormat.getDateTimeInstance().format(new Date(Lock.END_MILLIS)) + ")");

        text("\n🚫 Bloklangan:\n"
                + " • Standoff 2\n • eFootball\n • Brawl Stars\n"
                + " • Play Market / Galaxy Store'dagi BARCHA o'yinlar\n"
                + " • Telegram va boshqa joydan APK o'rnatish\n\n"
                + "🔒 Himoya:\n"
                + " • Ilovani o'chirib bo'lmaydi\n"
                + " • Sozlamalardan sbros (factory reset) bloklangan\n"
                + " • Sana/vaqtni o'zgartirib bo'lmaydi\n"
                + " • Xavfsiz rejim va ADB o'chirilgan\n\n"
                + "✅ Instagram, Telegram va boshqa oddiy ilovalar ishlaydi.\n"
                + "22-dekabr kelganda qulf o'zi avtomatik ochiladi.\n"
                + "Parol yo'q: muddatidan oldin hech kim ocha olmaydi.");

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::render, 60_000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    // ---- kichik UI yordamchilari ----
    private void title(String s) { TextView t = text(s); t.setTextSize(28); t.setTypeface(null, Typeface.BOLD); }
    private void big(String s) { TextView t = text(s); t.setTextSize(30); t.setGravity(Gravity.CENTER); t.setTypeface(null, Typeface.BOLD); }
    private void section(String s) { TextView t = text("\n" + s); t.setTextSize(20); t.setTypeface(null, Typeface.BOLD); }
    private void mono(String s) { TextView t = text(s); t.setTypeface(Typeface.MONOSPACE); t.setTextIsSelectable(true); }

    private TextView text(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(16);
        t.setPadding(0, dp(6), 0, dp(6));
        root.addView(t);
        return t;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(e);
        return e;
    }

    private void button(String s, Runnable r) {
        Button b = new Button(this);
        b.setText(s);
        b.setOnClickListener(v -> r.run());
        root.addView(b);
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
