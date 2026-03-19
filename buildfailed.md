❮ ./gradlew testDebugUnitTest --max-workers=1 | tee test-output.log
Reusing configuration cache.
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:generateDebugBuildConfig UP-TO-DATE
> Task :app:generateDebugResources UP-TO-DATE
> Task :app:injectCrashlyticsMappingFileIdDebug UP-TO-DATE
> Task :app:injectCrashlyticsVersionControlInfoDebug UP-TO-DATE
> Task :app:processDebugGoogleServices UP-TO-DATE
> Task :app:packageDebugResources UP-TO-DATE
> Task :app:processDebugNavigationResources UP-TO-DATE
> Task :app:parseDebugLocalResources UP-TO-DATE
> Task :app:generateDebugRFile UP-TO-DATE
> Task :app:kspDebugKotlin
> Task :app:compileDebugKotlin
w: file:///home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/battery/BatteryCapacityReader.kt:10:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/storage/MediaStoreScanner.kt:21:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/storage/StorageCleanupHelper.kt:16:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/emma/dev/runcheck/app/src/main/java/com/runcheck/data/storage/ThumbnailLoader.kt:16:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/emma/dev/runcheck/app/src/main/java/com/runcheck/service/monitor/ScreenStateTracker.kt:20:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/emma/dev/runcheck/app/src/main/java/com/runcheck/ui/common/UiText.kt:16:25 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/emma/dev/runcheck/app/src/main/java/com/runcheck/ui/pro/ProUpgradeScreen.kt:141:91 Unnecessary non-null assertion (!!) on a non-null receiver of type 'String'.

> Task :app:javaPreCompileDebug
> Task :app:compileDebugJavaWithJavac
> Task :app:hiltSyncDebug
> Task :app:hiltAggregateDepsDebug
> Task :app:hiltJavaCompileDebug
> Task :app:checkDebugAarMetadata
> Task :app:mapDebugSourceSetPaths
> Task :app:compileDebugNavigationResources
> Task :app:mergeDebugResources
> Task :app:createDebugCompatibleScreenManifests
> Task :app:extractDeepLinksDebug
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:processDebugManifestForPackage
> Task :app:processDebugResources
> Task :app:transformDebugClassesWithAsm
> Task :app:bundleDebugClassesToRuntimeJar
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:processDebugJavaRes
> Task :app:bundleDebugClassesToCompileJar
> Task :app:kspDebugUnitTestKotlin

> Task :app:compileDebugUnitTestKotlin FAILED
31 actionable tasks: 22 executed, 9 up-to-date
Configuration cache entry reused.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/scoring/HealthScoreCalculatorTest.kt:64:9 No parameter with name 'mediaBytes' found.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:29:35 Unresolved reference 'CalculateHealthScoreUseCase'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:66:9 No parameter with name 'mediaBytes' found.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:81:19 Unresolved reference 'CalculateHealthScoreUseCase'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:92:21 Unresolved reference 'useCase'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:92:31 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:94:26 Unresolved reference 'overallScore'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:95:26 Unresolved reference 'batteryScore'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:96:26 Unresolved reference 'networkScore'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:97:26 Unresolved reference 'thermalScore'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:98:26 Unresolved reference 'storageScore'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:103:21 Unresolved reference 'useCase'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:103:31 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:105:50 Unresolved reference 'status'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:106:26 Unresolved reference 'overallScore'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:126:19 Unresolved reference 'CalculateHealthScoreUseCase'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:134:21 Unresolved reference 'useCase'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:134:31 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:136:62 Unresolved reference 'overallScore'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:136:84 Unresolved reference 'overallScore'.
e: file:///home/emma/dev/runcheck/app/src/test/java/com/runcheck/domain/usecase/CalculateHealthScoreUseCaseTest.kt:137:31 Unresolved reference 'networkScore'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugUnitTestKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 2m 2s
