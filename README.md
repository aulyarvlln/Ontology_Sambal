# 🌶️ Ontologi Sambal Nusantara

## Web Semantik IF-A
- Nayla Cahaya Zahira (123240026)
- Sukmawati Kharisma Gati (123240137)
- Aulya Revalina (123240141)
- Nathania Keyne Rahmawati (123240201)

Repositori ini berisi rancangan model ontologi (*OWL*), file basis pengetahuan, dan kumpulan kueri SPARQL untuk sistem rekomendasi serta klasifikasi berbagai jenis sambal tradisional. Proyek ini dikembangkan untuk memenuhi tugas mata kuliah **Web Semantik**.

## Struktur Ontologi & Kelas
Model ontologi ini dibangun menggunakan **Protégé** dengan hierarki kelas utama meliputi:
* **`JenisSambal`**: Mencakup *SambalMatang* dan *SambalMentah*.
* **`BahanSambal`**: Terdiri dari *BahanPedas* (Jenis Cabai), *BahanPenyedap*, dan *PeralatanPembuatan*.
* **`TeknikPengolahan`**: Mencakup *TeknikPemasakan*, *TeknikPenghancuran*, dan *TeknikPengawetan*.
* **`KarakteristikSambal`**: Meliputi *DisajikanUntuk* (*MakananBerat*, *Cemilan*) dan *TingkatKepedasan*.

## Properti Ontologi
* **Object Properties**: Menghubungkan antar entitas, seperti `:DibuatDenganAlat`, `:MenggunakanBahanPedas`, `:MenggunakanTeknikPenghancuran`, dan `:DisajikanUntuk`.
* **Data Properties**: Menyimpan atribut teks/nilai dari entitas, seperti `:NamaSambal`, `:AsalDaerah`, `:RasaSambal`, `:TeksturSambal`, dan `:WarnaSambal`.
