# 🛡 Imtihon Qalqoni

Samsung Galaxy S25 (Android 15) uchun ilova. U **2026-yil 22-dekabrgacha** o'yinlarni bloklaydi.

## Nima qiladi

| Funksiya | Qanday ishlaydi |
|---|---|
| Standoff 2, eFootball, Brawl Stars | O'rnatilishi bilan darhol bloklanadi va o'chiriladi |
| Boshqa barcha o'yinlar | Play Market'da "Game" toifasidagi ilovalar ham o'chiriladi |
| Telegram'dan APK | "Noma'lum manbalar" butunlay o'chirilgan |
| Ilovaning o'zini o'chirish | Bloklangan (Device Owner + `setUninstallBlocked`) |
| Factory reset (Sozlamalardan) | Bloklangan |
| Sana/vaqtni o'zgartirish | Bloklangan (avtomatik vaqt majburiy) |
| Safe mode, ADB, yangi foydalanuvchi, Secure Folder | Bloklangan |
| Instagram, Telegram, YouTube… | Ishlaydi ✅ |
| **22-dekabr 00:00** | Qulf **o'zi avtomatik** ochiladi, keyin ilovani oddiy o'chirasiz |

## O'rnatish (bir marta, kompyuter kerak)

1. **APK'ni yuklab oling:** GitHub → *Releases* → `ImtihonQalqoni.apk`, keyin telefonga o'rnating.
2. **Telefondagi hamma akkauntlarni vaqtincha o'chiring:** *Sozlamalar → Akkauntlar va zaxira → Akkauntlarni boshqarish*, Google va Samsung akkauntlari. Android shuni talab qiladi, keyin qayta qo'shasiz.
   - Secure Folder bo'lsa, uni ham o'chiring.
3. **USB debugging'ni yoqing:** *Telefon haqida → Dasturiy ta'minot ma'lumotlari → "Build number"* ustiga 7 marta bosing. Keyin *Developer options → USB debugging*.
4. Kompyuterga [Platform Tools (adb)](https://developer.android.com/tools/releases/platform-tools) o'rnating, telefonni USB bilan ulang va buyruqni bajaring:
   ```
   adb shell dpm set-device-owner uz.focus.examlock/.AdminReceiver
   ```
   `Success: Device owner set to package ...` chiqsa, tayyor.
5. Ilovani oching. Parol yo'q: qulfni 22-dekabrdan oldin hech kim, hatto siz ham ocha olmaysiz.
6. Google va Samsung akkauntlaringizni qayta qo'shing.

## Ochilish
- **22-dekabr 00:00 (Toshkent vaqti)** kelganda hamma cheklov o'zi olib tashlanadi. Ilovani ochib, "Ilovani o'chirish" tugmasini bosasiz.

## Cheklovlar (halol aytaman)
- Telefonni **Recovery rejimida** (Power + Volume Up) to'liq tozalash baribir mumkin. Lekin bunda telefondagi **barcha ma'lumotlar** o'chib ketadi.
- O'yin o'rnatilganidan keyin 1–2 soniya ichida o'chiriladi. Play Market'dan "yuklab olish" tugmasi bosiladi, lekin o'yin ishlamaydi.
