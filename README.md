# CardVault

**CardVault 1.2** – helyi, titkosított bankkártyaadat-tároló Androidra, kézi Android Autofill-kitöltéssel.

## Röviden

A CardVault a kártyaszámot és a lejárati dátumot titkosítva tárolja a készüléken. Az Android Automatikus kitöltés (Autofill) szolgáltatásán keresztül a felhasználó kérésére felajánlja ezeket az adatokat az engedélyezett alkalmazásokban. A kitöltés előtt minden alkalommal erős biometrikus hitelesítést kér.

- Nincs felhőszinkronizálás vagy hálózati adatküldés a kártyaadatokhoz.
- A CVC/CVV-kódot az alkalmazás nem menti tartósan és nem tölti ki automatikusan.
- A normál mezőfókusz önmagában **nem** jelenít meg CardVault-ajánlatot. A felhasználónak az Android „Automatikus kitöltés” műveletét kell kiválasztania.

## Funkciók

- Kártyaszám és lejárati dátum titkosított tárolása.
- AES-256-GCM titkosítás, a kulcs az Android Keystore-ban készül és nem exportálható.
- Kitöltésenként friss `BIOMETRIC_STRONG` ujjlenyomatos azonosítás.
- Kézi Autofill-ajánlat bármilyen támogatott, fókuszban lévő szövegmezőhöz; nem kizárólag kártyaadatként felismert mezőkhöz.
- Csak a felhasználó által kiválasztott aktuális mező töltődik ki.
- Engedélyezett alkalmazások listája és keresése név vagy csomagnév alapján.
- Külön másolásgomb a kártyaszámhoz, lejárathoz és a csak ideiglenesen megadott CVC-hez.
- Sötét mód, Material You dinamikus színek (Android 12+), illetve egyéni kiemelőszínek.

## Telepítés

1. Töltsd le a GitHub Actions legutóbbi sikeres futásának `CardVault` nevű artifactját.
2. Csomagold ki az artifactot, majd telepítsd a benne található **`CardVault.apk`** fájlt.
3. Nyisd meg a CardVaultot, és végezd el a készülék biometrikus azonosítását.
4. Add meg a kártyaszámot és a lejárati dátumot, majd mentsd el.
5. Nyisd meg az **Engedélyezett alkalmazások** oldalt, és engedélyezd például a Steamet.
6. A CardVault főoldalán válaszd a **CardVault beállítása kitöltőként** lehetőséget, majd az Android beállításaiban válaszd ki a CardVaultot automatikus kitöltési szolgáltatásként.

## Használat

1. Nyisd meg az engedélyezett alkalmazást, és koppints a kitölteni kívánt szövegmezőre.
2. A billentyűzet vagy a rendszer szövegmező-menüjének **Automatikus kitöltés** műveletét válaszd ki.
3. Az Android megjeleníti a CardVault két lehetőségét: **Kártyaszám** és **Lejárati dátum**.
4. Válaszd ki a kívánt adatot, majd igazold magad ujjlenyomattal.
5. Sikeres hitelesítés után a CardVault közvetlenül az Android Autofill-rendszernek adja vissza az adatot; a célalkalmazás marad előtérben.

A CardVault szándékosan nem ajánlja fel magát automatikusan pusztán a mezőre koppintáskor. Ezért az Autofill-ajánlat csak a kézi Android-kitöltési művelet után jelenik meg.

## Kártyaadatok és biztonság

- A kártyaszám és lejárat AES-256-GCM-mel titkosított, a kulcsot az Android Keystore védi.
- A kitöltési adatokhoz minden egyes alkalommal új erős biometrikus hitelesítés szükséges.
- A CVC/CVV-kódot a CardVault nem tárolja tartósan. A CVC-másolásgomb csak az alkalmazásban ideiglenesen megadott kódot tudja másolni.
- A vágólapra másolt érzékeny adatot használat után érdemes azonnal törölni.
- Az alkalmazásadatok Android-mentése ki van kapcsolva; az alkalmazás eltávolítása vagy adatainak törlése a helyi kártyaadatokat is eltávolíthatja.

**Fontos:** a kártyaadatok helyi titkosítása csökkenti a kockázatot, de nem teszi kockázatmentessé a kártyaadatok telefonon tárolását. A készüléket tartsd naprakészen, használj erős képernyőzárat, és csak saját, megbízható készüléken használd.

## Kompatibilitás és korlátozások

- Minimum Android 9 (API 28); a projekt Android SDK 36-tal készül.
- Az alkalmazásnak át kell adnia a fókuszban lévő mezőt az Android Autofill-keretrendszernek. Egyes alkalmazások, webes nézetek vagy egyedi mezők ezt letilthatják; ilyen esetben egyetlen Autofill-szolgáltatás sem tudja kikényszeríteni a kitöltést.
- A CardVault csak az engedélyezett alkalmazásokban válaszol a kézi kitöltési kérésre.
- A szolgáltatás csak a fókuszban lévő mezőt tölti ki; a lejárati dátumot is külön kell kiválasztani a megfelelő mezőnél.
- A CVC automatikus kitöltése nincs támogatva.

## Fejlesztés és GitHub Actions build

A projekt GitHub Actions segítségével fordítható. A workflow JDK 17-et, Android SDK 36-ot és Gradle 8.11.1-et használ.

- Automatikus build: push a `main` ágra.
- Kézi build: **Actions → Build CardVault APK → Run workflow**.
- A workflow `assembleDebug` feladatot futtat, majd az eredményt `CardVault.apk` néven tölti fel `CardVault` artifactként.

A forráskód a `app/src/main/` könyvtárban található.
