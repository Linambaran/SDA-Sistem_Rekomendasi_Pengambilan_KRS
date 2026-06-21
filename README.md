# Implementasi Graf untuk Sistem Rekomendasi Pengambilan KRS dengan Kendala Batas SKS

Prototype program berbasis Java GUI (Swing) yang merekomendasikan mata kuliah untuk Kartu Rencana Studi (KRS) mahasiswa berdasarkan graf kurikulum, prasyarat akademik, dan jatah SKS. Proyek ini mengimplementasikan konsep **Directed Acyclic Graph (DAG)** dan algoritma **Greedy** menggunakan *Priority Queue*.

**Studi Kasus:** Kurikulum Program Studi Sains Data tahun akademik 2023/2024, Universitas Sebelas Maret (UNS).

## Main Features

- **GUI (Java Swing)**, filter mata kuliah yang dapat ditambah/dihapus berdasarkan semester mahasiswa saat ini.
- **Validasi prasyarat (in-degree)**, mencegah pengambilan mata kuliah jika mata kuliah prasyarat di bawahnya belum berstatus lulus (in-degree mata kuliah != 0).
- **Membatasi SKS secara dinamis**, dimana jatah maksimal SKS dihitung berdasarkan IP semester sebelumnya (Skala 18, 20, atau 24 SKS).
- **Penanganan Syarat Khusus Kurikulum:**
  - Mata kuliah akhir (Skripsi/KKN/Magang) hanya dapat diambil jika total SKS yang telah diselesaikan adalah **minimal 84**.
  - Mata kuliah MBKM & Pilihan dikunci, hanya dapat diambil oleh mahasiswa yang telah mencapai batas minimal semester (Semester 5/6).
- **Validasi *Disjoint Set*** yang mencegah mata kuliah yang sama masuk ke dalam daftar "Lulus" dan "Mengulang" secara bersamaan.

## Arsitektur Algoritma

Teori Graf dan Struktur Data yang diimplementasikan pada program ini:

1. **Pemodelan Kurikulum (DAG):** Seluruh mata kuliah dipetakan sebagai *Vertices* (Simpul), dan relasi prasyarat dipetakan sebagai *Directed Edges* (Panah).
2. **Bobot Masa Depan (DFS Critical Path):** Algoritma *Depth First Search* (DFS) menyusuri graf untuk menghitung seberapa "kritis" sebuah mata kuliah. Mata kuliah yang membuka banyak prasyarat di masa depan akan mendapat prioritas lebih tinggi untuk direkomendasikan.
3. **Filtering & Sinkronisasi (Fase 2):** Sistem mengalkulasi total `In-Degree` untuk menentukan gembok prasyarat. Status "Lulus" dari input pengguna akan memicu `In-Degree--` pada mata kuliah lanjutannya.
4. **Alokasi Greedy (Priority Queue):** Mata kuliah yang bebas syarat (`In-Degree == 0`) dimasukkan ke dalam antrean. Antrean diseleksi menggunakan *Greedy Algorithm* berdasarkan:
   - Status Mengulang (Prioritas Tertinggi)
   - Bobot Masa Depan Tertinggi (DFS)
   - Bobot SKS Tertinggi

## Bug Fixes

- **"Floating Nodes":** Mencegah mata kuliah tanpa panah prasyarat (seperti *Metode Penelitian* di Semester 6) "melompat" ke rekomendasi semester bawah dengan menerapkan *filter* Plafon Semester.
- **"Chain-Taking Bug":** Memodifikasi algoritma *Topological Sort* tradisional pada fase pengambilan antrean untuk mencegah mahasiswa mengambil mata kuliah prasyarat dan mata kuliah lanjutannya (misal: Metpen & Skripsi) di satu semester yang bersamaan.

## How To RUN (README!!!)

**Pre-requirement:** Java Development Kit (JDK) 8 or latest.

1. *Clone* repositori ini:
   ```bash
   git clone https://github.com/Linambaran/SDA-Sistem_Rekomendasi_Pengambilan_KRS.git
2. Change directory (cari direktori clone):
   ```bash
   cd SDA-Sistem_Rekomendasi_Pengambilan_KRS
3. Compile program:
   ```bash
   javac MainFrame.java
4. Run:
   ```bash
   java MainFrame

## Screenshot Program

<img width="1916" height="1078" alt="image" src="https://github.com/user-attachments/assets/cc92d191-39a6-41be-89bb-fbb3e7dd7aea" />

Developed as a final project for Data Structure and Algorithm & Discrete Math.

