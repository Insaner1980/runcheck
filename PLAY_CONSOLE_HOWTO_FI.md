# runcheck Play Console -ohje hyvin yksinkertaisesti

Tama tiedosto on tehty niin, etta sen voi lukea myohemmin ilman paniikkia.
Ajatus on tama:

- ensin tehdaan appi valmiimmaksi
- sitten testataan turvallisesti
- vasta lopuksi taytetaan Play Consoleen lopulliset tiedot

Tama ei ole juridinen lausunto. Tama on kaytannon muistilista `runcheck`-projektia varten.

## 1. Tarkeimmat sanat ihan helposti

### Internal testing

Tama on pieni salainen testipaikka.

- sinne voi laittaa appin nopeasti
- sita voi testata itse tai muutama tuttu
- tama EI ole sama asia kuin Googlen vaatimus `12 testaajaa / 14 paivaa`

### Closed testing

Tama on isompi testipaikka ennen oikeaa julkaisua.

- jos Play-tilisi kuuluu uuden henkilokohtaisen tilin vaatimukseen, productioniin paasy vaatii taman
- silloin tarvitset vahintaan `12` testaajaa
- heidan taytyy pysya mukana `14` paivaa putkeen

### Production

Tama on oikea julkaisu.

- vasta tanne appi menee kaikkien ladattavaksi

## 2. Ovatko billing-testi ja 12 testaajaa sama asia?

Eivat ole.

Billing-testi tarkoittaa:

- kokeilen toimiiko `Pro`-oston nappi oikeasti
- kokeilen aukeaako `runcheck_pro`
- kokeilen muistetaanko osto oikein

`12 testaajaa / 14 paivaa` tarkoittaa:

- Googlen julkaisulupaa productioniin
- se liittyy `closed testing` -raitaan
- se ei ole sama kuin billingin toiminnan tarkistus

Lyhyesti:

- billing-testi = "toimiiko osto"
- 12 testaajaa = "saanko julkaista productioniin"

## 3. Milloin mita kannattaa tehda?

Jos appiin tulee viela ominaisuuksia, tee asiat tassa jarjestyksessa:

1. rakenna ominaisuudet rauhassa
2. pidä `internal testing` kaytossa omia testejasi varten
3. testaa billing silloin kun haluat varmistaa Pro-oston
4. tee Data safety lopulliseksi vasta lahempana oikeaa julkaisua
5. tee `closed testing` vasta kun ominaisuudet alkavat oikeasti vakaantua

## 4. Voiko billingia testata ilman oikeaa rahaa?

Voi.

Tarkeaa:

- oma Google-tili taytyy lisata `License testing` -listaan Play Consolessa
- silloin kayttoon tulevat testimaksutavat
- silloin ostosta ei pitaisi menna oikeaa rahaa

Jos tili EI ole license tester:

- testiradalla tehty osto voi veloittaa oikeasti

## 5. Billing-testi askel askeleelta

Tee nain:

1. Avaa Play Console.
2. Mene kohtaan `Monetize with Play > Products > In-app products`.
3. Luo one-time product nimella `runcheck_pro`.
4. Aktivoi tuote.
5. Mene kohtaan `Settings > License testing`.
6. Lisaa oma Gmail-osoitteesi sinne.
7. Tee jompikumpi:
- helpoin tapa: testaa sideloadatulla buildilla license tester -tililla
- realistisempi tapa: laita appi `internal testing` -raidalle ja asenna se Play Storen kautta
8. Avaa `runcheck`.
9. Mene Pro-ostoon.
10. Tarkista, etta ostoruudussa nakyy testimaksutapa tai testiosto.
11. Tee osto.
12. Tarkista:
- Pro aukeaa heti
- appin uudelleenkaynnistys ei poista Prota
- samaa non-consumable-tuotetta ei voi ostaa uudestaan normaalisti

Jos haluat testata saman tuotteen uudestaan:

- kayta Play Consolessa refund/revoke testin jalkeen

## 6. Milloin billing kannattaa testata?

Billing kannattaa testata jo ennen julkaisua, vaikka appi ei olisi valmis.

Hyva hetki billing-testille:

- kun Pro-nappi on valmis
- kun `runcheck_pro` on luotu Play Consoleen
- kun haluat varmistaa, ettei julkaisussa tule yllatysta

Billingia EI tarvitse jattaa aivan viime tinkaan.

## 7. Data safety hyvin yksinkertaisesti

Ajattele nain:

- jos data pysyy vain puhelimessa, se on yksi asia
- jos data lahtee puhelimesta ulos verkkoon, se on toinen asia

Data safetyssa kaikkein tarkein kysymys on:

`lahteeko jotain tietoa pois laitteesta?`

Jos vastaus on `kylla`, asia taytyy arvioida.

## 8. Kannattaako Data safety tehda nyt?

Tee nyt mieluummin luonnos, ei lopullista versiota.

Hyva tapa:

1. tee data-inventaario koodista
2. paivita sita aina kun lisat uusia ominaisuuksia
3. tayta lopullinen Data safety vasta lahempana `closed testing` / `production` -vaihetta

Syy on yksinkertainen:

- jos lisaat viela ominaisuuksia, lopullinen vastaus voi muuttua
- silloin et joudu tekemaan samaa tyota monta kertaa alusta

## 9. Mita `runcheck`issa kannattaa muistaa Data safetysta?

Tama on tarkea:

- `runcheck` ei ole ihan kokonaan offline-appi
- speed test lahettaa dataa ulos
- latency-mittaus lahettaa dataa ulos
- billing-SDK liikkuu verkossa

Eli lopullinen vastaus ei todennakoisesti ole vain:

`ei kerää dataa`

Mutta kaikkea ei tarvitse arvata etukateen.
Siksi erillinen data-inventaario on paras pohja.

## 10. Mita `FOREGROUND_SERVICE_SPECIAL_USE` tarkoittaa?

Se tarkoittaa sita, etta appissa on foreground service, jota Android ja Play haluavat ymmartaa tarkemmin.

`runcheck`issa tama liittyy live-notification-monitorointiin.

Ajattele sita nain:

- kayttaja laittaa toiminnon paalle
- appi nayttaa jatkuvaa ilmoitusta
- ilmoituksessa on elavaa tietoa

Play Console haluaa tietaa:

- mita ominaisuutta tama tekee
- mita haittaa tulee jos se ei kaynnisty heti
- mita haittaa tulee jos Android keskeyttaa sen
- miten kayttaja itse kaynnistaa taman ominaisuuden

## 11. Miten video tehdaan?

Tee se omalla puhelimella.

Se riittaa hyvin.

Tee nain:

1. laita puhelimessa screen recording paalle
2. avaa `runcheck`
3. mene `Settings`-nakymaan
4. laita live notification paalle
5. nayta, etta ilmoitus ilmestyy
6. halutessasi laita se viela pois paalta ja uudestaan paalle
7. lopeta tallennus

Sitten:

- lataa video esim. Google Driveen tai YouTubeen `unlisted`-linkilla
- laita se linkki Play Consoleen

Et tarvitse hienoa editointia.
Tarkein asia on, etta videosta nakee:

- mita kayttaja painaa
- mita ominaisuus tekee

## 12. Voinko tehda videon omalla puhelimella?

Kyllä voit.

Se on itse asiassa todennakoisesti paras tapa, koska:

- video on oikealta laitteelta
- Playlle nayttaa luonnolliselta
- saat helposti juuri sen kayttajapolun talteen, jonka oikea kayttajakin tekee

## 13. Miten muistan mita teen ensin?

Muista tama lyhyt jarjestys:

1. rakenna ominaisuudet
2. testaa billing turvallisesti
3. pidä data-inventaario ajan tasalla
4. tee video vasta kun ominaisuus ei enaa muutu jatkuvasti
5. tee Data safety lopulliseksi lahempana julkaisua
6. tee closed test ja mahdollinen 12 testaajan vaihe vasta kun appi on melkein valmis

## 14. Mina tekisin nyt nain

Jos olisin sinä, tekisin nyt vain taman:

1. jatkan `runcheck`in ominaisuuksia
2. säästän taman ohjeen talteen
3. teen billing-testin heti kun haluan varmistaa Pro-oston
4. en tee lopullista Data safety -lomaketta viela
5. pidän data-inventaarion mukana projektissa ja paivitan sita aina kun verkkoon lahetetaan jotain uutta

## 15. Viralliset lahteet

- Google Play testing requirements: <https://support.google.com/googleplay/android-developer/answer/14151465?hl=en>
- Google Play Billing testing: <https://developer.android.com/google/play/billing/test>
- In-app / one-time product availability: <https://support.google.com/googleplay/android-developer/answer/1153481?hl=en>
- Data safety form guidance: <https://support.google.com/googleplay/android-developer/answer/10787469?hl=en>
- Foreground service declaration: <https://support.google.com/googleplay/android-developer/answer/13392821?hl=en>
