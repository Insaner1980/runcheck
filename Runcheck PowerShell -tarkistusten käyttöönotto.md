# Runcheck PowerShell -tarkistusten käyttöönotto

> Historiallinen käyttöönotto-suunnitelma. Nykyinen build- ja wrapper-totuus on `PROJECT.md`, `AGENTS.md`, `CODEX.md` sekä live-tiedostot `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml` ja `tools\*.ps1`.

## Yhteenveto
- Tavoite: samat PowerShell-lyhenteet toimivat `C:\Dev\runcheck`-repossa paikallisen `tools\*.ps1`-komentoperheen kautta ja käyttävät yhteistä `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1`-moduulia.
- Alkuperäinen lähtötila suunnitelman kirjoitushetkellä: runcheckissä ei ollut `tools`-kansiota; profiilisi haki nimenomaan `tools\<alias>.ps1`; CodeQL-workflow ja `sonar-project.properties` olivat olemassa; Dependabot-, Semgrep-, MobSF-, DeepSec-, OWASP-, Sonar/Jacoco-, Compose Stability- ja Google Security Lint -wiring oli puutteellinen.
- Toteutuksessa ei kosketa nykyisiä likaisia käyttäjämuutoksia ilman erillistä tarvetta. `reports/` jää ignoroiduksi.

## Julkinen Komentorajapinta
- Lisää `tools\lc.ps1`, `ac.ps1`, `dc.ps1`, `ss.ps1`, `ds.ps1`, `ms.ps1`, `os.ps1`, `ql.ps1`, `db.ps1`, `pc.ps1`, `cs.ps1`, `cr.ps1`, `ga.ps1` ja `sc.ps1`.
- Jokainen wrapper on ohut välitin: se asettaa oikean `ProjectCheckCommand`-arvon ja kutsuu `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1 @args`.
- `ad/install-debug` jätetään pois, koska et listannut sitä tähän kokonaisuuteen.
- Lisää `tools\sonar.ps1`, joka lukee `sonar-project.properties`, vaatii `SONAR_TOKEN`-arvon analyysia varten, ajaa Gradlen Sonar/Jacoco-polun ja kirjoittaa `reports\sonar.txt`.
- Muuta nykyinen `scripts\security-check.ps1` yhteensopivuuswrapperiksi `tools\sc.ps1`:lle, jotta vanha suora kutsu ei jää duplikoiduksi rinnakkaiseksi tarkistuslogiikaksi. `scripts\security-check.sh` jätetään Linux-legacyksi.

## Toteutusmuutokset
- Päivitä `gradle\libs.versions.toml`, `build.gradle.kts` ja `app\build.gradle.kts` niin, että wrapperien odottamat tehtävät ovat olemassa:
  - OWASP Dependency-Check Gradle plugin `12.2.2`.
  - SonarQube Gradle plugin `7.3.1.8318`.
  - compose-rules: Detekt-polulle `0.4.28`, ktlint-polulle `0.5.9`.
  - Google Android Security Lints `1.0.4`.
  - Compose Stability Analyzer `0.7.4`, ei uusinta `0.9.0`:aa, koska `0.9.0` on Kotlin 2.4.0 -linjaa ja runcheck on Kotlin 2.3.0.
- Lisää `dependencyCheck { ... }` app-moduuliin: raportit `reports\`, data `.gradle\dependency-check-data`, `debugRuntimeClasspath` ja `releaseRuntimeClasspath`, CVSS-raja envillä yliajettava, NVD-viiveet envillä yliajettavat.
- Lisää `jacocoDebugUnitTestReport` ja rootin `sonar`-wiring niin, että Sonar saa XML-coveragen eikä jää 0%-tilaan.
- Lisää `config\semgrep\runcheck-security.yml` runcheckin riskeille: backup/cleartext, exported komponenttien review, FileProviderin liian leveät polut ja arkaluonteinen lokitus.
- Lisää `.mobsf` runcheckille: ignoraa build/cache/report-polut ja dokumentoi `android_task_hijacking2` false positive, koska target SDK tulee Gradlesta eikä lähdemanifestista.
- Lisää `.github\dependabot.yml` Gradle- ja GitHub Actions -ekosysteemeille viikoittaisella aikataululla.
- Lisää `.deepsec`-scaffold olemassa olevasta Android-mallista ja sovita matcherit runcheckille: exported komponentit, FileProvider, URI-jako, arkaluonteinen lokitus, verkkokutsupinnat ja release-telemetrian rajaus.
- Päivitä `AGENTS.md` ja `CODEX.md` samaan tilaan: uusi komentoperhe, raporttitiedostot, “älä aja raskaita tarkistuksia ilman käyttäjän pyyntöä”, sekä se että käyttäjä ajaa varsinaiset `lc`/`sc`-skannit.

## Testisuunnitelma
- Kevyt savutesti ilman raskaita skanneja: aja jokaiselle wrapperille `-PlanOnly` ja varmista, että se tulostaa oikean tarkistussuunnitelman.
- Gradle-konfiguraatiotesti: aja vain tehtävälistaus ja varmista, että `:app:dependencyCheckAnalyze`, `:app:stabilityCheck`, `:app:ktlintCheck`, `:app:detekt`, `:app:lint`, `:app:jacocoDebugUnitTestReport` ja `sonar` näkyvät.
- Dependency verification: luo `gradle\verification-metadata.xml` harkitusti `dc -InitVerification` -polulla, tarkista diff ja committoi vain relevantti metadata.
- Varsinaiset hyväksyntäajot käyttäjän terminaalissa: `lc`, `dc`, `ss`, `ac`, `pc`, `cr`, `ga`, `cs`, `ql`, `db`, `sonar`, lopuksi `sc` ja tarvittaessa `sc -Full`.
- Jokaisen ajon jälkeen korjaa vain raportin todelliset löydökset; älä vaimenna sääntöjä ennen kuin false positive on todistettu.

## Oletukset Ja Lähteet
- Oletus: toteutus tehdään Windows 11 / PowerShell -ensisijaisesti, mutta vanhaa bash-skriptiä ei poisteta.
- Oletus: runcheckin Kotlin-versiota ei nosteta tässä työssä.
- Viralliset versiolähteet tarkistettu 2026-06-06: [OWASP Gradle plugin](https://plugins.gradle.org/plugin/org.owasp.dependencycheck), [SonarQube Gradle plugin](https://plugins.gradle.org/plugin/org.sonarqube), [Compose Stability Analyzer releases](https://github.com/skydoves/compose-stability-analyzer/releases), [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html), [CodeQL compiled languages](https://docs.github.com/en/code-security/how-tos/find-and-fix-code-vulnerabilities/manage-your-configuration/codeql-code-scanning-for-compiled-languages), [Dependabot alerts API](https://docs.github.com/rest/dependabot/alerts/).
