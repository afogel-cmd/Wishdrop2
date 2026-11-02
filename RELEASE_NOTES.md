# WishDrop v1.0.2 Kiadási Megjegyzések (Release Notes)

### 🚀 Újdonságok és Fejlesztések (What's New)
- **Kívánságlista tömeges kijelölése és törlése (Batch Wishlist Deletion):**
  - Mostantól egyszerre több tételt is ki lehet jelölni és törölni a kívánságlistából.
  - Dedikált kijelölési mód lebegő műveleti sávval (floating action bar), kijelölt elemek számlálójával, "Mind kijelölése" / "Kijelölés törlése" lehetőséggel és megerősítő párbeszédpanellel.
- **Teljeskörű MediaMarkt link támogatás telefonon is (MediaMarkt Link Extraction):**
  - Intelligens mobil-böngésző fejléc- és átirányításkezelés, amely megbízhatóan átviszi a kéréseket és megkerüli az agresszív anti-bot blokkolásokat.
  - Pontos cím, ár (HUF), kategóriák és nagyfelbontású hivatalos CDN termékképek (`assets.mmsrg.com`) automatikus betöltése a beágyazott JSON-LD és GraphQL adatokból.
  - Zökkenőmentes magyar ékezetes karakter- és slug-feloldás (`ő`, `ű`, `á`, `é`, `í`, `ó`).
- **Közvetlen Android Megosztás (Share to WishDrop):**
  - A rendszer megosztási menüjéből (`ACTION_SEND`) közvetlenül megnyitható a WishDrop: böngészőből (pl. Chrome) vagy más appokból megosztott terméklinkek azonnal a hozzáadási képernyőre kerülnek, és elindul az automatikus AI elemzés.
- **Kifinomultabb vizuális illusztrációk és tartalékok (Fallback Artworks):**
  - Ha egy külső bolt képe nem elérhető vagy hálózati hiba lép fel, az intelligens illusztrációs rendszer automatikusan a terméktípusnak megfelelő (pl. telefon, TV, audio, háztartási gép, ruha, konzol) dizájnt jelenít meg a generikus ikon helyett.

---

# WishDrop v1.0.1 Kiadási Megjegyzések (Release Notes)

### 🚀 Korábbi fejlesztések (Previous Highlights in v1.0.1)
- Automatikus alkalmazásfrissítés-kezelő (In-App Updater GitHub Releases integrációval).
- Közös listameghívások elutasításának lehetősége.
- Google fiók profilkép szinkronizáció és megbízható helyi/felhő avatar-kezelés.
- Érdeklődési körök mentésének és megjelenítésének javítása.
