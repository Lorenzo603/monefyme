---
status: DO NOT RUN
attempts: 1
title: "fix compilation warnings"
source: backlog.md
depends_on: nothing

## Task

Run this on macos to fix path of files first. run `./gradlew clean` first








Fix the following warnings that were displayed when running this command which required a recompilation: `./gradlew test`

```
> Task :app:compileReleaseKotlin
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/data/local/AppDatabase.kt:29:34 The corresponding parameter in the supertype 'Migration' is named 'db'. This may cause problems when calling this function with named arguments.
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/category/CategoryIconRegistry.kt:51:53 'DirectionsBike: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Outlined.DirectionsBike
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt:147:41 'ArrowBack: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Outlined.ArrowBack
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt:169:9 'Divider(Modifier = ..., Dp = ..., Color = ...): Unit' is deprecated. Renamed to HorizontalDivider
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt:190:9 'Divider(Modifier = ..., Dp = ..., Color = ...): Unit' is deprecated. Renamed to HorizontalDivider
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt:200:9 'Divider(Modifier = ..., Dp = ..., Color = ...): Unit' is deprecated. Renamed to HorizontalDivider
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt:204:17 'Divider(Modifier = ..., Dp = ..., Color = ...): Unit' is deprecated. Renamed to HorizontalDivider
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt:217:9 'Divider(Modifier = ..., Dp = ..., Color = ...): Unit' is deprecated. Renamed to HorizontalDivider
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt:393:30 Parameter 'activeCount' is never used
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt:397:36 Parameter 'activeCount' is never used
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt:160:41 'ArrowBack: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Outlined.ArrowBack
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/screen/AddTransactionScreen.kt:233:17 'Divider(Modifier = ..., Dp = ..., Color = ...): Unit' is deprecated. Renamed to HorizontalDivider
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/screen/RestoreScreen.kt:70:45 'ArrowBack: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Outlined.ArrowBack
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/screen/TransactionsScreen.kt:68:45 'ArrowBack: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Outlined.ArrowBack
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/viewmodel/MainViewModel.kt:32:10 This declaration needs opt-in. Its usage should be marked with '@kotlinx.coroutines.ExperimentalCoroutinesApi' or '@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)'
w: file:///C:/p/monefyme/app/src/main/java/com/lorenzo/monefyme/ui/viewmodel/MainViewModel.kt:36:10 This declaration needs opt-in. Its usage should be marked with '@kotlinx.coroutines.ExperimentalCoroutinesApi' or '@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)'

> Task :app:kaptReleaseUnitTestKotlin
warning: The following options were not recognized by any processor: '[room.schemaLocation, kapt.kotlin.generated, room.incremental, room.expandProjection]'

> Task :app:compileReleaseUnitTestKotlin
w: file:///C:/p/monefyme/app/src/test/java/com/lorenzo/monefyme/ui/viewmodel/MainViewModelFactoryTest.kt:52:20 Check for instance is always 'true'
```