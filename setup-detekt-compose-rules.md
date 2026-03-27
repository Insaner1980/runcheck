# Task: Add Detekt + Compose Rules to runcheck

## What This Is

Set up Detekt (static code analysis for Kotlin) and Compose Rules (Jetpack Compose-specific lint rules) in the runcheck Android project. These tools catch code quality issues, common Compose mistakes, and potential bugs automatically — without running the app.

## Versions to Use

- **Detekt**: `1.23.8` (latest stable, plugin id: `io.gitlab.arturbosch.detekt`)
- **Compose Rules**: `io.nlopez.compose.rules:detekt:0.4.27` (latest for detekt 1.x)

Do NOT use Detekt 2.x (alpha) or Compose Rules 0.5.x (which targets Detekt 2.x).

## Steps

### 1. Add Detekt Gradle Plugin

In version catalog (`libs.versions.toml`), add:

```toml
[versions]
detekt = "1.23.8"

[plugins]
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }

[libraries]
detekt-compose-rules = { module = "io.nlopez.compose.rules:detekt", version = "0.4.27" }
```

In root `build.gradle.kts`, apply the plugin:

```kotlin
plugins {
    alias(libs.plugins.detekt) apply false
}
```

In `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    parallel = true
}

dependencies {
    detektPlugins(libs.detekt.compose.rules)
}
```

### 2. Generate Default Config, Then Customize

Run `./gradlew detektGenerateConfig` to create the default config file at `config/detekt/detekt.yml`.

Then add/modify these sections in the generated file:

#### Compose-friendly overrides for built-in Detekt rules

These prevent false positives on normal Compose patterns:

```yaml
naming:
  FunctionNaming:
    # Composable functions use PascalCase
    ignoreAnnotated:
      - 'Composable'
  TopLevelPropertyNaming:
    # Compose uses PascalCase for top-level constants like Colors
    constantPattern: '[A-Z][A-Za-z0-9]*'

complexity:
  LongParameterList:
    # Composables often have many params with defaults
    ignoreAnnotated:
      - 'Composable'
    ignoreDefaultParameters: true
  TooManyFunctions:
    # Files with many @Preview functions are fine
    ignoreAnnotated:
      - 'Preview'

style:
  MagicNumber:
    # Color hex values and dp/sp values are fine
    ignoreAnnotated:
      - 'Composable'
    ignorePropertyDeclaration: true
  UnusedPrivateMember:
    # Preview composables are "unused" but needed
    ignoreAnnotated:
      - 'Preview'
  ForbiddenComment:
    # Allow TODO comments during development
    active: false
```

#### Enable Compose Rules

Add this section to enable the compose-rules plugin:

```yaml
Compose:
  ComposableAnnotationNaming:
    active: true
  ComposableNaming:
    active: true
  ComposableParamOrder:
    active: true
  ContentEmitterReturningValues:
    active: true
  DefaultsVisibility:
    active: true
  ModifierComposable:
    active: true
  ModifierMissing:
    active: true
  ModifierReused:
    active: true
  ModifierWithoutDefault:
    active: true
  MultipleEmitters:
    active: true
  MutableParams:
    active: true
  CompositionLocalAllowlist:
    active: false
  CompositionLocalNaming:
    active: true
  ContentTrailingLambda:
    active: true
  LambdaParameterInRestartableEffect:
    active: true
  Material2:
    active: false
  PreviewAnnotationNaming:
    active: true
  PreviewPublic:
    active: true
  RememberMissing:
    active: true
  RememberContentMissing:
    active: true
  UnstableCollections:
    active: false
  ViewModelForwarding:
    active: true
  ViewModelInjection:
    active: true
```

### 3. Create Baseline (Important)

Since the codebase already exists and likely has many findings, generate a baseline file so Detekt only reports NEW issues going forward:

```bash
./gradlew detektBaseline
```

This creates `app/detekt-baseline.xml`. Commit this file. Then update `app/build.gradle.kts`:

```kotlin
detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("detekt-baseline.xml")
    parallel = true
}
```

The baseline means existing code won't trigger failures, but any new code Claude Code writes will be checked against the rules.

### 4. Verify It Works

Run:

```bash
./gradlew detekt
```

It should complete without errors (baseline suppresses existing findings). Check the HTML report at `app/build/reports/detekt/detekt.html` to see what was baselined.

### 5. What NOT to Do

- Do NOT add Detekt to any CI/CD pipeline (there isn't one yet)
- Do NOT set `allRules = true` — the defaults plus Compose Rules are enough
- Do NOT configure `failOnSeverity` or make the build fail on warnings yet — start with reporting only
- Do NOT use the detekt formatting ruleset (it conflicts with other formatters)
