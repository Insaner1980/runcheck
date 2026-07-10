# runcheck Material 3 Refinement Plan

**Summary**
Toteutetaan Fable 5:n `m3-refinement-plan.md` cleanissä worktreessä `C:\Users\emmah\.config\superpowers\worktrees\runcheck\ulkoasu-uudistus`, ei nykyisessä dirty-checkoutissa. Muutos koskee vain Compose-ulkoasua: spacing, shape, surface-hierarkia, kortit, button/chip-rytmi, tilakortit ja screen-kohtainen tiheys. Ei muuteta värejä, typografiaa, mittauslogiikkaa, Pro-gatingia, navigaatioreittejä, chart-piirtologiikkaa, dynamic coloria tai teemoja.

Viralliset lähteet tarkistettu ennen suunnittelua: [Compose Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3), [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3), [Compose chips](https://developer.android.com/develop/ui/compose/components/chip). Repo käyttää Compose BOMia ja `material3`-kirjastoa jo nyt, joten riippuvuuspäivitystä ei sisällytetä tähän visuaaliseen suunnitelmaan.

**Implementation Changes**
1. **Design-token pohja**
   - Muokkaa `app/src/main/java/com/runcheck/ui/theme/Shapes.kt`: `large` 16dp -> 20dp, `medium` 8dp -> 12dp, lisää `HeroCardShape = RoundedCornerShape(28.dp)`, ja muuta `BottomSheetShape` käyttämään 28dp top-cornereita.
   - Muokkaa `Spacing.kt`: lisää `cardInner = 20.dp`; säilytä nykyinen 4dp-grid: 2/4/8/12/16/24/32.
   - Muokkaa `UiTokens.kt`: `iconCircle` 44->48, `iconCircleInner` 22->24, `compactIconCircle` 36->40, `badgeVerticalPadding` 4->6, `proBadgeHorizontalPadding` 8->10, `proBadgeVerticalPadding` 3->4.
   - Älä muuta `Color.kt`, `StatusColors.kt`, `Type.kt` tai `MotionTokens.kt`.

2. **Shared component -sweep**
   - `GridCard`: poista status strip -käyttö ja ehdollinen start-padding; käytä yhtenäistä 16dp paddingia, 20dp shapea, 40dp icon circleä ja jätä status vain labeliin/ikonitinttiin.
   - `ActionCard`: poista `runcheckOutlinedCardBorder()`, vaihda container `surfaceContainerHigh`, tee koko kortista klikattava, säilytä `actionLabel` saavutettavuuslabelina, korvaa näkyvä `TextButton` chevron-pillillä.
   - `ListRow`: lisää `ListRowVariant.Value/Navigation`; `Navigation` käyttää 56dp min-heightia, 40dp `IconCircle`ä ja nykyistä chevron-pilliä 4dp sisäpaddingilla. Nykyinen value-row säilyy oletuksena.
   - `MetricPill.kt`: säilytä nykyinen `MetricPill`; lisää `MetricBlock`, joka käyttää `surfaceContainerHigh`, `MaterialTheme.shapes.medium`, 12dp paddingia, 4dp label/value-gapin ja `onSurface`-värisiä arvoja.
   - Lisää `StateCard`: standardi loading/empty/error/locked/permission-kortti, jossa on 20dp korttishape, 20dp padding, valinnainen 48dp `IconCircle` tai spinner, title, body ja pill-muotoinen action.
   - `ProFeatureCalloutCard` ja `ProFeatureLockedState`: käytä `StateCard`-kielioppia, pill `OutlinedButton`ia ja 48dp lock icon circleä; älä muuta Pro-logiikkaa.
   - `SegmentedBar`: 12dp -> 16dp height, gap 2dp -> 3dp, corner 6dp -> 8dp, legend dot 8dp -> 10dp ja legend row gap 4dp -> 8dp.
   - `SegmentedStatusBar`: oletus 6dp -> 8dp, gap 3dp -> 4dp, labels gap 4dp -> 8dp.
   - `HeatStrip`: 24dp -> 32dp, shape pysyy pillimäisenä.
   - `SignalBars`: corner 3dp -> 4dp.
   - `InfoCard`, `CrossLinkButton`, `ProBadgePill`, chart expand buttons ja CTA-napit: käytä 20dp/28dp/pill-shape-ladderia, mutta älä muuta sisältöä.

3. **Screen-erät**
   - **Home:** normalisoi health hero `HeroCardShape`en; siirrä health breakdown erilliseen 20dp korttiin heti heron alle; poista GridCard-stripit; Quick Tools käyttää `ListRowVariant.Navigation`; pidä nykyinen järjestys ja 600dp wide-switch.
   - **Battery / Network / Thermal / Storage:** kaikki hero-kortit 24dp padding + `HeroCardShape`; hero-metriikat max kolme `MetricBlock`ia; poista dividerit korteista, joissa on enintään neljä riviä; säilytä pitkät details-taulukot dividerillisinä.
   - **Network:** jaa connection details kahdeksi kortiksi: WiFi/cellular facts ja IP/DNS/general facts; pidä copyable `MetricRow`-käytös.
   - **Speed Test:** pidä gauge-piirto ennallaan; muuta live metrics ja latest/result stats 2x2 `MetricBlock`-grideiksi; values `onSurface`, ei primary-väriä.
   - **Storage/Cleanup:** borderless cleanup `ActionCardit`, paksumpi `SegmentedBar`, cleanup rows/headerit 12dp vertical paddingilla, delete button pilliksi.
   - **App Usage:** korvaa N erillistä appikorttia yhdellä list-group-kortilla; säilytä paging, permission ja Pro-flow.
   - **Charger:** comparison bars 8dp -> 12dp pill-bars, selected charger hero-lite `surfaceContainerHigh`, dialogit 28dp shapeen.
   - **Insights / Learn / Settings / Pro / Fullscreen:** Insights horizontal padding 24->16; Learn topic spacing 4->24; Settings card padding 20 ja master toggles 56dp; Pro feature list tonal cardiin ja buy button pilliksi; Fullscreenissa vain chip-gap 4->8 ja empty state `StateCard`iksi.

4. **Docs and guardrails**
   - Päivitä toteutuksen lopuksi `UI-SPEC.md`, `docs/ui-reference.md` ja `docs/ui-consistency-audit.md` nykyisestä koodista.
   - Pidä `UI-SPEC.md` code-derived: ei Fable-prosessitekstiä, ei modernization-promptia, ei suunnittelumuistiinpanoja.
   - Jos toteutus lisää pysyviä repo-ohjeita design-tokenien käytöstä, päivitä sekä `AGENTS.md` että `CODEX.md` samassa diffissä. Muuten niitä ei kosketa.
   - Älä commitoi `reports/`-kansiota. Älä aja `lc`/`sc`-wrappereita itse.

**Test Plan**
- Ensin staattinen tarkistus: `rg` ettei uusia kovakoodattuja 20/28/48dp-arvoja toistu ruuduissa tokenien ohi, ja ettei uusia värihexejä tai `Icons.Default/Filled/Rounded`-käyttöjä tullut.
- Focused compile: `.\gradlew.bat :app:compileDebugKotlin`.
- UI-säännöt source-contract-testeinä, jos muutos on laaja: token values, `ActionCard` ei käytä outline borderia, `GridCard` ei käytä `StatusStrip`iä, `BottomSheetShape` on 28dp.
- Analyzerit vain suoraan ja tarvittaessa: `.\gradlew.bat :app:ktlintCheck`, `.\gradlew.bat :app:detekt`, `.\gradlew.bat :app:lintDebug`.
- Visuaalinen hyväksyntä: emulaattori-/screenshot-kierros Home, Battery, Network, Speed Test, Thermal, Storage, Cleanup, App Usage, Settings, Pro, Fullscreen; tarkista erityisesti ei päällekkäistä tekstiä, yksi status carrier per komponentti, touch targetit vähintään 48dp ja reduced-motion-polut ennallaan.

**Assumptions**
- Toteutus tehdään `codex/ulkoasu-uudistus`-worktreessä, jotta `C:\Dev\runcheck` ja sen nykyiset untracked/muokatut tiedostot säilyvät erillään.
- `m3-refinement-plan.md` on suunnittelulähde, mutta ei automaattisesti osa clean worktreeä; jos sitä tarvitaan toteutuksen aikana, se luetaan alkuperäisestä polusta tai tallennetaan erikseen sovittuun docs-polkuun.
- Ensimmäinen toteutuserä kannattaa rajata tokeneihin + shared componentteihin + Homeen. Sen jälkeen ruudut voidaan tehdä yksi kokonaisuus kerrallaan, jotta ulkoasua voi arvioida ennen koko sovelluksen sweepiä.
