package uz.focus.examlock;

import android.Manifest;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Butun qulf mantig'i shu yerda. */
public final class Lock {
    private static final String TAG = "ExamLock";

    /** Qulf tugash vaqti: 2026-yil 22-dekabr, 00:00 (Toshkent vaqti). */
    public static final long END_MILLIS = ZonedDateTime
            .of(2026, 12, 22, 0, 0, 0, 0, ZoneId.of("Asia/Tashkent"))
            .toInstant().toEpochMilli();

    /** Asosiy taqiqlangan o'yinlar (Play Market paket nomlari). */
    public static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "com.axlebolt.standoff2",      // Standoff 2
            "jp.konami.pesam",             // eFootball
            "com.supercell.brawlstars"     // Brawl Stars
    ));

    /** Qulf davomida yoqilgan cheklovlar. */
    private static final String[] RESTRICTIONS = {
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,           // Telegram'dan APK o'rnatib bo'lmaydi
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
            UserManager.DISALLOW_FACTORY_RESET,                      // Sozlamalardan sbros qilib bo'lmaydi
            UserManager.DISALLOW_SAFE_BOOT,                          // Xavfsiz rejimga kirib bo'lmaydi
            UserManager.DISALLOW_DEBUGGING_FEATURES,                 // ADB orqali aylanib o'tib bo'lmaydi
            UserManager.DISALLOW_CONFIG_DATE_TIME,                   // Sanani 22-dekabrga surib bo'lmaydi
            UserManager.DISALLOW_ADD_USER,                           // Yangi foydalanuvchi ochib bo'lmaydi
            UserManager.DISALLOW_ADD_MANAGED_PROFILE,                // Ish profili / Secure Folder
            UserManager.DISALLOW_ADD_CLONE_PROFILE,
    };

    private static final String PREFS = "examlock";
    private static final long PASSWORD_SETUP_WINDOW = 15 * 60 * 1000L;

    private Lock() {}

    public static ComponentName admin(Context c) {
        return new ComponentName(c, AdminReceiver.class);
    }

    static DevicePolicyManager dpm(Context c) {
        return c.getSystemService(DevicePolicyManager.class);
    }

    public static boolean isOwner(Context c) {
        return dpm(c).isDeviceOwnerApp(c.getPackageName());
    }

    public static boolean isExpired() {
        return System.currentTimeMillis() >= END_MILLIS;
    }

    static SharedPreferences prefs(Context c) {
        return c.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Asosiy funksiya: muddat tugamagan bo'lsa qulfni qo'yadi, tugagan bo'lsa olib tashlaydi. */
    public static void enforce(Context c) {
        if (!isOwner(c)) return;
        if (isExpired()) {
            release(c);
            return;
        }
        SharedPreferences p = prefs(c);
        if (!p.contains("enabled_at")) p.edit().putLong("enabled_at", System.currentTimeMillis()).apply();

        DevicePolicyManager dpm = dpm(c);
        ComponentName a = admin(c);
        for (String r : RESTRICTIONS) safe(() -> dpm.addUserRestriction(a, r));
        safe(() -> dpm.setAutoTimeEnabled(a, true));
        safe(() -> dpm.setAutoTimeZoneEnabled(a, true));
        safe(() -> dpm.setUninstallBlocked(a, c.getPackageName(), true));
        if (Build.VERSION.SDK_INT >= 33) {
            safe(() -> dpm.setPermissionGrantState(a, c.getPackageName(),
                    Manifest.permission.POST_NOTIFICATIONS,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED));
        }

        scanAll(c);
        GuardService.start(c);
        scheduleJob(c);
    }

    /** 22-dekabrdan keyin (yoki favqulodda parol bilan) hamma cheklovni olib tashlaydi. */
    public static void release(Context c) {
        if (!isOwner(c)) return;
        DevicePolicyManager dpm = dpm(c);
        ComponentName a = admin(c);
        for (String r : RESTRICTIONS) safe(() -> dpm.clearUserRestriction(a, r));
        safe(() -> dpm.setUninstallBlocked(a, c.getPackageName(), false));
        safe(() -> {
            String[] suspended = prefs(c).getStringSet("suspended", new HashSet<>()).toArray(new String[0]);
            if (suspended.length > 0) dpm.setPackagesSuspended(a, suspended, false);
        });
        c.getSystemService(JobScheduler.class).cancelAll();
        c.stopService(new Intent(c, GuardService.class));
        prefs(c).edit().putBoolean("released", true).apply();
        safe(() -> dpm.clearDeviceOwnerApp(c.getPackageName()));
        Log.i(TAG, "Qulf olib tashlandi");
    }

    // ---------------- O'yinlarni aniqlash va o'chirish ----------------

    static boolean isGame(ApplicationInfo ai) {
        if (ai == null) return false;
        if (BLOCKED.contains(ai.packageName)) return true;
        if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) return false;           // tizim ilovalariga tegmaymiz
        if (ai.category == ApplicationInfo.CATEGORY_GAME) return true;
        //noinspection deprecation
        return (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0;
    }

    public static void handlePackage(Context c, String pkg) {
        if (pkg == null || pkg.equals(c.getPackageName()) || !isOwner(c) || isExpired()) return;
        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(pkg, 0);
            if (isGame(ai)) removeGame(c, pkg);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }

    public static void scanAll(Context c) {
        if (!isOwner(c) || isExpired()) return;
        List<ApplicationInfo> apps = c.getPackageManager().getInstalledApplications(0);
        for (ApplicationInfo ai : apps) {
            if (!ai.packageName.equals(c.getPackageName()) && isGame(ai)) removeGame(c, ai.packageName);
        }
    }

    private static void removeGame(Context c, String pkg) {
        Log.i(TAG, "O'yin topildi, o'chirilmoqda: " + pkg);
        DevicePolicyManager dpm = dpm(c);
        // 1) Darhol ochilmaydigan qilamiz
        safe(() -> {
            dpm.setPackagesSuspended(admin(c), new String[]{pkg}, true);
            Set<String> s = new HashSet<>(prefs(c).getStringSet("suspended", new HashSet<>()));
            s.add(pkg);
            prefs(c).edit().putStringSet("suspended", s).apply();
        });
        // 2) Device Owner sifatida jimgina o'chiramiz
        safe(() -> {
            Intent i = new Intent(c, UninstallResultReceiver.class).putExtra("pkg", pkg);
            PendingIntent pi = PendingIntent.getBroadcast(c, pkg.hashCode(), i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            c.getPackageManager().getPackageInstaller().uninstall(pkg, pi.getIntentSender());
        });
    }

    static void scheduleJob(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        JobInfo job = new JobInfo.Builder(1, new ComponentName(c, ScanJob.class))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build();
        safe(() -> js.schedule(job));
    }

    // ---------------- Favqulodda parol ----------------

    public static boolean hasPassword(Context c) {
        return prefs(c).contains("pw_hash");
    }

    /** Parolni faqat Device Owner yoqilgandan keyingi 15 daqiqa ichida o'rnatish mumkin. */
    public static boolean canSetPassword(Context c) {
        if (hasPassword(c)) return false;
        long at = prefs(c).getLong("enabled_at", 0);
        return at > 0 && System.currentTimeMillis() - at < PASSWORD_SETUP_WINDOW;
    }

    public static boolean setPassword(Context c, String pw) {
        if (!canSetPassword(c) || pw == null || pw.length() < 6) return false;
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltHex = hex(salt);
        prefs(c).edit().putString("pw_salt", saltHex).putString("pw_hash", hash(saltHex, pw)).apply();
        return true;
    }

    public static boolean checkPassword(Context c, String pw) {
        SharedPreferences p = prefs(c);
        String salt = p.getString("pw_salt", null), h = p.getString("pw_hash", null);
        if (salt == null || h == null || pw == null) return false;
        // Har noto'g'ri urinishdan keyin kutish vaqti ortadi
        long lockedUntil = p.getLong("pw_locked_until", 0);
        if (System.currentTimeMillis() < lockedUntil) return false;
        boolean ok = MessageDigest.isEqual(h.getBytes(StandardCharsets.UTF_8),
                hash(salt, pw).getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            int fails = p.getInt("pw_fails", 0) + 1;
            long wait = Math.min(24 * 3600_000L, 60_000L * (1L << Math.min(fails, 10)));
            p.edit().putInt("pw_fails", fails).putLong("pw_locked_until", System.currentTimeMillis() + wait).apply();
        }
        return ok;
    }

    public static long passwordLockedUntil(Context c) {
        return prefs(c).getLong("pw_locked_until", 0);
    }

    private static String hash(String salt, String pw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = (salt + ":" + pw).getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < 50_000; i++) d = md.digest(d);
            return hex(d);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    static void safe(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            Log.w(TAG, "Xato: " + t);
        }
    }
}
