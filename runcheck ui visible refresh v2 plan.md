# runcheck Visible UI Refresh v2 Plan

## Summary

Tavoite on tehdä ero heti nähtäväksi puhelimessa. Edellinen toteutus oli teknisesti oikein, mutta liian hienovarainen: se muutti lähinnä radius-, spacing- ja korttihierarkiaa. v2 tehdään **Brand refresh** -tasolla: säilytetään runcheckin tumma, data-forward-identiteetti, mutta rakennetaan näkyvämpi first-viewport-rakenne, vahvemmat hero-pinnat, selkeämmät metric-tile-ryhmät ja uudelleenryhmitelty Home/detail-hierarkia.

Viralliset lähteet tarkistettu suunnittelua varten: [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3), [Compose Card](https://developer.android.com/develop/ui/compose/components/card), [Compose Chip](https://developer.android.com/develop/ui/compose/components/chip), [Compose Material 3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3).

## Public UI Interfaces

- Lisää shared-komponentti `RuncheckHeroDeck`: suuri ensimmäisen viewportin hero-pinta, jossa on title/status, yksi primary metric, valinnainen visual slot ja 2-3 `MetricBlock`-tukea.
- Lisää shared-komponentti `RuncheckSectionDeck`: isompi ryhmittelykortti, joka sisältää otsikon, valinnaisen summary-rivin ja content-slotin.
- Laajenna `MetricBlock`: lisää `emphasis`-variantit `Primary`, `Secondary`, `Compact`; oletuksena arvot pysyvät `onSurface`, accent vain pienessä markerissa.
- Lisää theme-tokenit keskitetysti, ei raw-arvoja ruutuihin: `heroDeckMinHeight`, `heroDeckPadding`, `metricTileMinHeight`, `sectionDeckGap`, `heroVisualSize`.
- Päivitä `UI-SPEC.md`, `docs/ui-reference.md` ja `docs/ui-consistency-audit.md` toteutuksen jälkeen. `AGENTS.md`/`CODEX.md` päivitetään vain jos lisätään pysyviä repo-ohjeita.

## Key Changes

- **Home first viewport:** korvaa nykyinen erillinen hero + grid -rakenne yhdellä näkyvällä dashboard-deckillä. Health score, status summary ja 2x2 status tiles kuuluvat samaan isoon hero-alueeseen, jotta ensimmäinen näkymä näyttää selvästi uudelta.
- **Detail heroes:** Battery, Network, Thermal ja Storage käyttävät samaa `RuncheckHeroDeck`-kielioppia: iso primary metric vasemmalla, visuaalinen ankkuri oikealla tai ylhäällä, ja 2-3 metric tileä samassa deckissä. Tämä tekee muutoksen näkyväksi ilman chart-piirtologiikan vaihtoa.
- **Secondary content:** siirrä pitkät details-/history-/actions-osat `RuncheckSectionDeck`-ryhmiin. Vähennä “korttiseinä”-tunnetta yhdistämällä lähisukulaiset yhteen deckiin, ei lisäämällä yksittäisiä pikkukortteja.
- **Visible interaction refresh:** kaikki pää-CTA:t ja tärkeät navigation/action-rivit käyttävät pill/chevron-kieltä, mutta Home/detail-näytöillä painopiste on nyt hero- ja deck-rakenteessa, ei pelkissä pienissä painikemuodoissa.
- **Constraints:** ei dynamic coloria, ei light/AMOLED-teemaa, ei mittauslogiikan, Pro-gatingin, navigaatioreittien, speed test -backendin tai chart-canvasien muutoksia. Paletti säilyy, mutta olemassa olevia väritokeneita saa käyttää kontrolloidummin isoissa pinnoissa theme-tokenien kautta.

## Test Plan

- Lisää `VisibleUiRefreshContractTest`, joka varmistaa että `RuncheckHeroDeck` on käytössä Homessa sekä Battery/Network/Thermal/Storage-detail-näytöissä, Home ei palaa vanhaan erilliseen hero+grid-muotoon, ja uudet tokenit ovat theme-tiedostoissa eivätkä ruuduissa raw-arvoina.
- Päivitä nykyinen `Material3RefinementContractTest`, jotta se sallii v2-komponentit mutta pitää guardrailit: ei uusia raw hex -värejä, ei `Icons.Default/Filled/Rounded`, ei dynamic coloria, ei `StatusStrip`-paluuta.
- Aja rajatusti: `.\gradlew.bat :app:ktlintCheck --max-workers=2`, `.\gradlew.bat :app:compileDebugKotlin --max-workers=2`, ja kohdennetut testit `Material3RefinementContractTest` + `VisibleUiRefreshContractTest`.
- Tee visuaalinen hyväksyntä oikealla laitteella tai emulaattorilla: Home, Battery, Network, Thermal ja Storage first viewport. Hyväksymiskriteeri: jokaisessa näkyy yhdellä vilkaisulla uusi iso hero/deck-rakenne, ei vain pyöreämmät kortit.
- Älä aja `lc`/`sc`-wrappereita tässä vaiheessa; käyttäjä ajaa ne halutessaan.

## Assumptions

- Toteutus jatkuu worktreessä `C:\Users\emmah\.config\superpowers\worktrees\runcheck\ulkoasu-uudistus`.
- Oletusvalinta on **Brand refresh**: näkyvämpi kuin strict M3 refinement, mutta ei full redesign.
- Värit ja typografia pysyvät pääosin nykyisinä, mutta layout saa muuttua rohkeasti first viewportissa.
- Ensimmäinen toteutuserä kannattaa rajata `RuncheckHeroDeck` + Home + yksi detail-screen, asentaa laitteelle ja arvioida näkyvyys ennen koko screen-sweepiä.
