# WishDrop v1.0.1 Kiadási Megjegyzések (Release Notes)

### 🚀 Újdonságok és Fejlesztések (What's New)
- **Automatikus alkalmazásfrissítés-kezelő (In-App Updater):**
  - Az alkalmazás induláskor automatikusan ellenőrzi a legújabb elérhető verziót a GitHub Releases-ből.
  - Új verzió esetén letisztult dialógusablakban jeleníti meg a kiadási megjegyzéseket (release notes), és közvetlenül az alkalmazásból letölthető és telepíthető a frissítés.
  - A Profil képernyőn a verzióinformációk mellett manuális "Frissítések keresése" lehetőség.
- **Közös listameghívások elutasítása (Decline Invitations):**
  - A beérkező családi kívánságlista meghívásokat mostantól nemcsak elfogadni lehet, hanem elutasítani is, mindezt külön elutasítási gombbal és letisztultabb UI elrendezéssel.
- **Google fiók integráció és profilkép behúzás:**
  - Egységesített Google bejelentkezési kártya a Profil felületen.
  - Automatikus profilkép betöltés Google fiókból vagy Gravatar / felhő alapú feloldással.
- **Megbízható helyi és felhő avatar-kezelés:**
  - A galériából választott profilképek mostantól biztonságosan az alkalmazás belső, privát tárhelyére kerülnek mentésre, így az Android Photo Picker jogosultságainak lejárta vagy appfrissítés után sem vesznek el a képek.
  - Felhőszinkronizáció javítása: a lokálisan beállított profilkép nem íródik felül üres értékkel a felhő szinkronizálásakor.

### 🐛 Hibajavítások és Stabilitás (Bug Fixes)
- Kijavítva a profilképek betöltési hibája (permission denial) és monogramra való visszaesése.
- Kijavítva az új érdeklődési körök megjelenítése a profilszerkesztőben és családtagok kártyáin.
- Tisztított, letisztult fiókstátusz a Profil képernyőn.
