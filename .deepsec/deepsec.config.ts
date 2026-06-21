import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { type DeepsecPlugin, defineConfig } from "deepsec/config";
import { androidExportedComponent } from "./matchers/android-exported-component.js";
import { androidUriShareWithoutClipData } from "./matchers/android-uri-share-without-clipdata.js";
import { fileproviderBroadPath } from "./matchers/fileprovider-broad-path.js";
import { outboundNetworkSurface } from "./matchers/outbound-network-surface.js";
import { releaseTelemetrySurface } from "./matchers/release-telemetry-surface.js";
import { sensitiveAndroidLog } from "./matchers/sensitive-android-log.js";

const here = path.dirname(fileURLToPath(import.meta.url));

function runcheckPlugin(): DeepsecPlugin {
  return {
    name: "runcheck-android",
    matchers: [
      androidExportedComponent,
      fileproviderBroadPath,
      androidUriShareWithoutClipData,
      outboundNetworkSurface,
      releaseTelemetrySurface,
      sensitiveAndroidLog,
    ],
  };
}

export default defineConfig({
  projects: [
    {
      id: "runcheck",
      root: "..",
      infoMarkdown: fs.readFileSync(path.join(here, "data", "runcheck", "INFO.md"), "utf-8"),
      promptAppend:
        "Prioritize Android exported components, FileProvider exports, URI grants, release telemetry boundaries, device/network data logging, and outbound network calls outside NDT7 latency or billing paths.",
      priorityPaths: [
        "app/src/main/AndroidManifest.xml",
        "app/src/main/java/com/runcheck/data/network/",
        "app/src/main/java/com/runcheck/data/billing/",
        "app/src/main/java/com/runcheck/billing/",
        "app/src/main/java/com/runcheck/service/monitor/",
        "app/src/release/java/com/runcheck/SentryInit.kt",
      ],
    },
  ],
  plugins: [runcheckPlugin()],
});
