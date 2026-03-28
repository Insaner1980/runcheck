# Puhelinverifiointi

Tama lista kokoaa ne tarkistukset, joita ei voinut sulkea pelkalla staattisella katselmoinnilla tai JVM-testeilla.

## 1. Screen ja power -eventit

- Avaa sovellus, kayta puhelimen naytto pois ja takaisin paalle useita kertoja.
- Varmista, etta Battery- tai Home-nakymassa screen-on/off-tilastot muuttuvat oikeaan suuntaan eika data jaa paikoilleen.
- Kytke laturi ja irrota se.
- Varmista, etta screen usage- ja sleep-analyysi nollaantyvat tai synkronoituvat odotetusti lataustilan vaihtuessa.

## 2. Doze / idle -kayttaytyminen

- Lukitse puhelin ja anna sen olla rauhassa niin, etta laite menee idle/doze-tilaan.
- Tarkista myohemmin, etta sleep analysis erottaa deep sleepin ja held awake -ajan jarkevasti.
- Toista sama latauksessa ja ilman laturia, jos mahdollista.

## 3. Boot ja package replace

- Kaynnista puhelin uudelleen.
- Varmista, etta monitorointi palaa kayntiin vain silloin kun asetukset sallivat sen.
- Asenna uusi debug-build vanhan paalle.
- Varmista, etta `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` -polut eivat riko monitorointia tai widgetteja.

## 4. HealthMonitorWorker oikeassa taustaymparistossa

- Jata sovellus taustalle useaksi monitorointijaksoksi.
- Tarkista, etta uudet snapshotit tallentuvat edelleen normaalisti.
- Pakota valiaikainen virhetilanne, jos mahdollista, esimerkiksi poistamalla tarvittava lupa tai rajoittamalla taustaa.
- Varmista, ettei sovellus yrita nostaa live notification -foreground servicea taustasta itsestaan.
- Varmista, etta stale-monitoring-indikaattori menee huonoksi vain oikeassa katkotilanteessa, ei satunnaisesti.

## 5. Live notification

- Ota live notification kayttoon Settingsista.
- Varmista, etta se kaynnistyy vain kayttajan omasta toimenpiteesta.
- Sulje sovellus, vaihda naytto pois ja paalle, kytke laturi, irrota laturi.
- Varmista, ettei worker tai muu taustapolku kaynnista sita uudelleen ilman kayttajan pyyntoa.

## 6. Pro unlock avoimessa ruudussa

- Avaa Battery Detail, Network Detail ja Thermal Detail ennen ostoa.
- Tee Pro-osto tai aktivoi trial/debug-polku avoimen ruudun aikana.
- Varmista, etta historia, lukitut osiot ja mahdolliset tyhjat free-tier-listat paivittyvat ilman screenin sulkemista ja avaamista uudelleen.
- Tarkista erikseen speed test -historian rajaus Network-nakymassa.

## 7. Widgetit

- Lisaa widget aloitusnaytolle ennen Pro-avausta ja sen jalkeen.
- Varmista, etta widget paivittyy oikein monitorointisnapshotien mukana.
- Tarkista kayttaytyminen bootin jalkeen.
- Tarkista, etta Pro-gating toimii johdonmukaisesti eika widget nayta vanhaa dataa unlockin jalkeen.

## 8. Process death / state restoration

- Avaa Battery-, Network- ja Thermal-nakymat.
- Vaihda history period, avaa fullscreen chart tai muu route-backed tila.
- Tapa sovellus recent-task-listasta tai adb:lla.
- Avaa sovellus uudelleen.
- Varmista, etta `SavedStateHandle`- ja `rememberSaveable`-tilat palautuvat oikein eika ruutu palaa virheelliseen oletustilaan.

## 9. Device capability / battery current -luotettavuus

- Tarkista ainakin yhdella oikealla laitteella, mieluiten Pixelilla ja yhdella valmistajaspesifilla laitteella kuten Samsung tai OnePlus.
- Varmista, etta current, charging-sign ja confidence-kayttaytyminen ovat jarkevia seka latauksessa etta purussa.
- Jos laite on paivittynyt uudempaan Android-versioon OTA:lla, varmista erityisesti ettei vanha capability-profiili jaa voimaan.

## 10. Mittarit joita ei voi sulkea puhtaasti puhelintestilla

- Room-migraatiot kannattaa ajaa erikseen instrumentaatiotesteina:
  - `RuncheckDatabaseMigrationTest`
- Tama ei ole puhelimen manuaalinen tarkistus, vaan AndroidTest/emulaattori- tai laiteajo.
