import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { regexCandidates } from "./utils.js";

export const releaseTelemetrySurface: MatcherPlugin = {
  slug: "release-telemetry-surface",
  description:
    "Telemetry, analytics, or crash reporting initialization in main or release source sets",
  noiseTier: "precise",
  filePatterns: ["app/src/main/java/**/*.kt", "app/src/release/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    const normalizedPath = filePath.replaceAll("\\", "/");
    if (normalizedPath.endsWith("app/src/release/java/com/runcheck/SentryInit.kt")) return [];

    return regexCandidates("release-telemetry-surface", content, [
      { regex: /\bSentryAndroid\.init\s*\(/, label: "Sentry init outside debug source set" },
      { regex: /\bFirebaseAnalytics\.getInstance\s*\(/, label: "Firebase Analytics in release/main source set" },
      { regex: /\bFirebaseCrashlytics\.getInstance\s*\(/, label: "Crashlytics in release/main source set" },
      { regex: /\bAnalytics\b/, label: "Analytics reference in release/main source set" },
    ]);
  },
};
