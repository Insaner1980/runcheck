import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

const sensitiveWords =
  "(?:battery|thermal|network|ssid|ip|dns|storage|device|billing|purchase|pro|token|uri|path|snapshot|insight|runcheck)";

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose device health, network, billing, export, or persisted diagnostic state",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt", "app/src/release/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("sensitive-android-log", content, [
      {
        regex: new RegExp(
          String.raw`\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e)\s*\([^;\n]*${sensitiveWords}[^;\n]*\)`,
          "i",
        ),
        label: "Sensitive device or account term in Android log call",
      },
    ]);
  },
};
