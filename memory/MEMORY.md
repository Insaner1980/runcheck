# runcheck memory

## Shared Android check architecture

- Check coverage is defined in `config/android-check.json`.
- Scanner exceptions are defined in `config/check-exceptions.json` and must remain exact, owned, tracked, and time-bounded.
- MobSF:n targetSdk-poikkeus suodatetaan yhteisessä Android-check-moottorissa vain säännölle `android_task_hijacking2` ja polulle `app/src/main/AndroidManifest.xml`; `.mobsf` ei sisällä globaalia rule-ignorea.
- Project wrappers delegate to `C:\Dev\Android-check`, publish reports atomically, and use exit 0 for clean, 1 for findings, and 2 for technical/configuration errors.
- 2026-08-03: PMD CPD refactoring centralized single-candidate insight evaluation in `SingleCandidateInsightRule` and specialized rule bases, and reusable Compose screen/card/chart structure in `ui/components/` and `ui/chart/`. New implementations should extend these shared primitives instead of duplicating their orchestration.
- 2026-08-03: Build-tool transitive security versions are centralized in `gradle.properties` under `runcheck.buildTools.*`. Root `build.gradle.kts` constrains only buildscript, ktlint, Android Lint, and Unified Test Platform configurations; app runtime graphs are not globally forced. The OSV config remains free of `PackageOverrides`.
- 2026-08-03: Tavallinen `ql` tarkistaa GitHubin oletushaaran CodeQL-baselinen ja nykyiset paikalliset Java/Kotlin/Gradle-inputit; lukittu paikallinen CLI ajetaan automaattisesti, ellei sama puhdas HEAD ole jo varmennetun remote-SHA:n kattama. `-CurrentCommit` säilyy legacy remote-scope -valintana. Sonar upload requires explicit `-AllowExternalUpload`.
- 2026-07-28: Projektikohtainen DeepSec päivitettiin täsmälleen versioon `2.2.9` sekä `package.json`issa että pnpm-lukituksessa. `tsc --noEmit` läpäisi; ulkoista AI-analyysiä ei ajettu.
