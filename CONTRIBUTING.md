# Panduan Berkontribusi di KalaOrder

Terima kasih atas ketertarikan Anda untuk ikut mengembangkan **KalaOrder**!

## Cara Berkontribusi

1. **Fork** repositori ini ke akun GitHub Anda.
2. Buat branch baru untuk fitur atau perbaikan bug Anda:
   ```bash
   git checkout -b fitur/nama-fitur-anda
   ```
3. Lakukan perubahan kode dengan menjaga gaya kode Kotlin & Jetpack Compose (Material 3) yang konsisten.
4. Pastikan proyek berhasil dikompilasi tanpa error:
   ```bash
   gradle :app:assembleDebug
   ```
5. Commit perubahan Anda dengan pesan yang jelas, lalu buat **Pull Request** ke branch `main`.

## Pelaporan Bug & Usulan Fitur

- Gunakan template **Bug Report** jika menemukan masalah, sertakan versi Android, merek HP, dan langkah reproduksi.
- Gunakan template **Feature Request** untuk mengusulkan fitur baru yang bermanfaat bagi kasir dan pelaku UMKM.

## Catatan Keamanan

- Jangan pernah melakukan commit file `.env`, `local.properties`, keystore rilis (`*.jks`), atau kredensial pribadi ke repositori.
