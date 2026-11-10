# CardVault v2 – titkosított kártyatároló + Android Autofill

## Funkciók
- Android Keystore AES-256-GCM, eszközhöz kötött titkosítás, 5 perces Keystore-hitelesítési időablakkal.
- Ujjlenyomat (BIOMETRIC_STRONG) vagy készülék-PIN/minta/jelszó. A csak gyenge biometrikusnak minősített arcfelismerés **nem** oldhatja fel a Keystore-kulcsot; ez tudatos biztonsági korlátozás.
- Kártyaszám és lejárat helyi titkosított tárolása; CVC kizárólag memóriában, az alkalmazás háttérbe kerülésekor törlődik.
- 3 külön másolásgomb, 30 másodperces vágólap-törlési kísérlettel. A vágólap tartalmát más alkalmazások vagy a rendszer korábban elérhetik, ezért a másolás nem tekinthető teljesen biztonságosnak.
- Android AutofillService, amely a felismerhető bankkártyaszám- és lejáratmezőkhöz kínál kitöltést, csak az engedélyezett alkalmazásokban, azonosítást követően.
- Engedélyezett alkalmazások képernyő; a rendszer Autofill-beállítását megnyitó gomb.
- Android 15/16 edge-to-edge státuszsáv / navigációs sáv biztonságos margó.
- FLAG_SECURE a képernyőképek/recents-előnézetek ellen; backup és eszközátvitel letiltva.

## Beállítás
1. APK telepítése és megnyitása; telefon képernyőzárának beállítása.
2. Hitelesítés, kártyaszám és lejárat megadása, **Kártyaadatok mentése**.
3. **Engedélyezett alkalmazások** oldalon jelöld be a Steamet (vagy más appot).
4. **CardVault beállítása kitöltőként**; válaszd ki a CardVaultot az Android rendszerben.
5. A fizetési mezőnél válaszd a CardVault-ajánlatot, majd hitelesíts.

## Fontos korlátok
- A Steam WebView-fizetési mezőinek Autofill-kompatibilitása nem garantált. Az alkalmazás csak az Android által továbbított és megbízhatóan felismerhető mezőket tölti ki.
- A CVC nem tárolódik, nem autofill-olható. Ha az appot elhagyod, az ideiglenes CVC törlődik.
- Ha az Android Keystore-kulcs elveszik (például készülékcsere, biztonsági beállítás változása), a titkosított adatok nem állíthatók helyre.
- Az app nem került helyi Android SDK-val fordításra vagy telefonon tesztelésre ebben a környezetben. GitHub Actions fordítás és valódi készülékes teszt szükséges.
- A helyi titkosítás nem garantál védelmet kompromittált/rootolt eszköz vagy kártékony billentyűzet ellen.
