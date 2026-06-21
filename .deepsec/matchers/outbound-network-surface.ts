import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

const allowedNetworkPaths = [
  "app/src/main/java/com/runcheck/data/network/",
  "app/src/main/java/com/runcheck/data/billing/",
  "app/src/main/java/com/runcheck/billing/",
];

export const outboundNetworkSurface: MatcherPlugin = {
  slug: "outbound-network-surface",
  description:
    "Outbound network primitives outside the approved speed test, latency, or billing paths",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    const normalizedPath = filePath.replaceAll("\\", "/");
    if (isTestFile(normalizedPath)) return [];
    if (allowedNetworkPaths.some((allowedPath) => normalizedPath.includes(allowedPath))) return [];

    return regexCandidates("outbound-network-surface", content, [
      { regex: /\bOkHttpClient\s*\(/, label: "OkHttpClient outside approved network or billing path" },
      { regex: /\bHttpURLConnection\b/, label: "HttpURLConnection outside approved network or billing path" },
      { regex: /\bURL\s*\([^)]*\)\.openConnection\s*\(/, label: "URL.openConnection outside approved network or billing path" },
      { regex: /\bSocket\s*\(/, label: "Socket outside approved network or billing path" },
      { regex: /\bInetAddress\.getByName\s*\(/, label: "DNS lookup outside approved network or billing path" },
    ]);
  },
};
