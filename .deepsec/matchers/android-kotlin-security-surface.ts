import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const androidKotlinSecuritySurface: MatcherPlugin = {
  slug: "android-kotlin-security-surface",
  description:
    "Kotlin Android component, widget, and background execution surfaces that deserve security review",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt", "app/src/release/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("android-kotlin-security-surface", content, [
      {
        regex: /\bclass\s+\w+[\s\S]{0,240}:\s*(?:[\w.]+\.)?GlanceAppWidgetReceiver\s*\(/,
        label: "Glance app widget broadcast receiver",
      },
      {
        regex: /\bclass\s+\w+[\s\S]{0,240}:\s*(?:[\w.]+\.)?(?:Service|LifecycleService)\s*\(/,
        label: "Android service entry point",
      },
      {
        regex: /\bclass\s+\w+[\s\S]{0,240}:\s*(?:[\w.]+\.)?(?:BroadcastReceiver)\s*\(/,
        label: "Android broadcast receiver entry point",
      },
      {
        regex: /\bclass\s+\w+[\s\S]{0,320}:\s*(?:[\w.]+\.)?(?:Worker|CoroutineWorker|ListenableWorker)\s*\(/,
        label: "WorkManager background execution entry point",
      },
      {
        regex: /\bWorkManager\.getInstance\s*\(|\benqueueUnique(?:Periodic)?Work\s*\(/,
        label: "WorkManager scheduling surface",
      },
    ]);
  },
};
