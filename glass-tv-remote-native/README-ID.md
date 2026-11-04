# Glass TV Remote — build dengan ADB native

Project ini mempertahankan antarmuka HTML yang Anda kirim dan mengganti bridge HTTP palsu port 6095 dengan plugin Kotlin ADB. APK belum dibangun/diuji di perangkat pada sesi ini; GitHub Actions akan melakukan build setelah source di-push.

## Cara pakai
1. Upload seluruh isi folder project ini ke repository GitHub (branch `main`).
2. Buka tab **Actions** → workflow **Build Glass TV APK** → tunggu selesai → unduh artifact `GlassTV-Remote-APK`.
3. Install APK di HP Android. HP dan Xiaomi TV harus di jaringan WiFi yang sama.
4. Pada TV buka Developer options → Wireless debugging.
5. Tekan Hubungkan di aplikasi. Pilih `2` untuk Android 11+ pairing: masukkan **IP TV**, **port pairing** dan **kode 6 digit**. Setelah pairing berhasil, masukkan **port koneksi** yang tertera di halaman Wireless debugging (port ini berbeda dari port pairing).
6. Jika TV menyediakan ADB TCP legacy port 5555, pilih `1`.

## Penting / keamanan
- Pairing ADB Android 11+ memakai TLS; port pairing dan port koneksi berbeda. Port berubah-ubah, jadi jangan menebak port.
- Perintah debloat memakai `pm disable-user --user 0`, bukan uninstall permanen. Restore memakai `pm enable`.
- Jangan menonaktifkan launcher PatchWall jika belum memasang launcher pengganti dan memastikan cara kembali ke launcher bawaan. Daftar paket harus dicek satu per satu karena tiap firmware Xiaomi berbeda.
- Tombol remote memakai `input keyevent` lewat ADB.
- Scan subnet HTTP sebelumnya tidak lagi dipakai sebagai bukti koneksi ADB; masukkan IP TV yang terlihat pada pengaturan jaringan TV.
- Library Kadb: https://github.com/flyfishxu/Kadb (Apache-2.0), digunakan untuk koneksi ADB langsung, pairing, dan shell.

## Batasan versi ini
- Pairing dan koneksi dilakukan manual melalui IP/port/kode yang ditampilkan TV; belum ada penemuan mDNS otomatis.
- APK hasil Actions belum dapat diklaim sukses sampai workflow hijau dan APK dipasang/diuji di HP serta TV.
- Konten web memakai CDN eksternal yang sudah ada di HTML sumber (Tailwind, Font Awesome, Google Fonts); tampilan tertentu perlu internet.
