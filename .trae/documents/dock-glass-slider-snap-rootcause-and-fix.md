# 底部 Dock 玻璃滑块「吸附到最近 tab」根因分析与修复计划

## 摘要

底部 dock 栏的玻璃滑块（thumb）在拖动时会「靠近哪个 tab 就吸附到哪个 tab」，而不是连续跟随手指停留在任意位置。

**根因**：app 内的上游适配副本 `LiquidBottomTabs.kt` 在拖动回调里把手指坐标经过 `tabIndexAtPosition(...)` 量化为**整数** tab 索引（`((x - padding) / tabWidth).toInt()`），导致每帧动画目标都跳变到某个整数 tab，滑块无法停留在 tab 之间。这与第三方参考实现（`third_party/backdrop-catalog/...`）使用**小数相对增量**的连续跟踪行为不同。

**修复**：在拖动路径改用新的**小数**映射 `tabFractionAtPosition(...)`（去掉 `.toInt()`），让 `dampedDragAnimation.value` 连续跟随手指；松手时仍由 `onDragStopped` 的 `dragTarget.fastRoundToInt()` 吸附到最近 tab 并提交选中——这正是用户选择的期望行为「拖动时连续跟随，松手吸附到最近 tab」。

## 当前状态分析（根因）

涉及文件：
- [LiquidBottomTabs.kt](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt)（app 适配副本，**问题来源**）
- [LiquidBottomTabs.kt](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/third_party/backdrop-catalog/components/LiquidBottomTabs.kt)（第三方参考，连续跟踪的原生行为）
- [DampedDragAnimation.kt](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/utils/DampedDragAnimation.kt)（弹簧动画驱动 `value`）
- [GlassDock.kt](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/GlassDock.kt)（`GlassBottomBar` 调用 `LiquidBottomTabs`）

### 数据流
玻璃滑块的水平位移由 `dampedDragAnimation.value`（`Float`，范围 `0..tabsCount-1`）驱动（见 [LiquidBottomTabs.kt:255-258](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L255-L258) 的 `translationX = dampedDragAnimation.value * tabWidth`）。`value` 由 `DampedDragAnimation.updateValue` 经弹簧 `spring(1f, 1000f, ...)` 追向 `targetValue`（见 [DampedDragAnimation.kt:113-118](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/utils/DampedDragAnimation.kt#L113-L118)）。

### 第三方参考的连续行为（正确）
[LiquidBottomTabs.kt:122-130](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/third_party/backdrop-catalog/components/LiquidBottomTabs.kt#L122-L130) `onDrag` 用**相对小数增量**：
```kotlin
onDrag = { _, dragAmount ->
    updateValue(
        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
    )
    ...
}
```
`dragAmount.x / tabWidth` 是小数增量，叠加到上一帧 `targetValue`，所以 `value` 可以是 `1.5` 之类的小数，滑块连续跟随手指、可停在两 tab 之间。松手时 `onDragStopped`（[:111-121](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/third_party/backdrop-catalog/components/LiquidBottomTabs.kt#L111-L121)）`targetValue.fastRoundToInt()` 吸附到最近 tab。

### app 适配副本的量化行为（问题）
[LiquidBottomTabs.kt:310-313](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L310-L313)：
```kotlin
internal fun tabIndexAtPosition(x, padding, tabWidth, count, isLtr): Int {
    val physical = ((x - padding) / tabWidth).toInt().coerceIn(0, count - 1)
    return if (isLtr) physical else count - 1 - physical
}
```
关键：`.toInt()` 截断为整数。

`onDragStarted`（[:118-122](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L118-L122)）与 `onDrag`（[:140-149](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L140-L149)）每帧把手指 x 经 `tabIndexAtPosition(...)` 重算为整数，再 `updateValue(dragTarget)`：
```kotlin
dragTarget = tabIndexAtPosition(position.x, ...).toFloat()   // 永远是整数
updateValue(dragTarget)
```
后果：
1. `dragTarget` 恒为整数 tab 索引，`updateValue` 让 `value` 弹向该整数 → 手指在 tab 边界附近时目标在两个整数间跳变，滑块表现为「靠近哪个 tab 就吸附」。
2. 滑块**无法**停留在两 tab 之间的任意手指位置，与用户期望的「手滑到哪就在哪」相悖。
3. `onDragStopped`（[:127-139](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L127-L139)）用 `dragTarget.fastRoundToInt()` 提交——但 `dragTarget` 已是整数，所以松手时不会有「最近 tab」的取整语义，而是直接锁定当前量化值。

> 结论：根因是适配副本在拖动路径上把连续坐标量化为整数（`tabIndexAtPosition` 的 `.toInt()`），偏离了参考实现的连续小数映射。

## 提议改动

仅改一个文件 + 其单元测试。所有改动均基于上方已读到的真实代码。

### 1. 新增小数映射 helper
文件：`app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt`，紧挨现有 `tabIndexAtPosition`（文件末尾 [:310](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L310)）后新增：
```kotlin
// 连续手指跟踪：返回小数 tab 索引，使玻璃滑块在拖动中可停在任意位置。
internal fun tabFractionAtPosition(x: Float, padding: Float, tabWidth: Float, count: Int, isLtr: Boolean): Float {
    val physical = ((x - padding) / tabWidth).coerceIn(0f, (count - 1).toFloat())
    return if (isLtr) physical else (count - 1).toFloat() - physical
}
```
**为什么**：与 `tabIndexAtPosition` 唯一区别是不做 `.toInt()`，保留小数，令 `value` 连续。保留 `tabIndexAtPosition`（仍被测试覆盖，表示「整数槽位」语义，便于将来 tap 逻辑复用）。

### 2. `onDragStarted` 改用小数映射
[:118-122](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L118-L122)：
```kotlin
onDragStarted = { position ->
    dragPosition = position
    dragTarget = tabFractionAtPosition(position.x, with(density) { 4.dp.toPx() }, tabWidth, tabsCount, isLtr)
    updateValue(dragTarget)
},
```
**为什么**：按下即把 thumb 跳到手指正下方的小数位置（而非量化到某 tab 中心），随后可连续拖动。一次轻点（无位移）时 `dragTarget≈i.0`，松手 `fastRoundToInt()` 仍选中该 tab——保留「点哪个 tab 选哪个」的整面板入口语义。

### 3. `onDrag` 改用小数映射
[:140-149](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L140-L149)：
```kotlin
onDrag = { _, dragAmount ->
    dragPosition += dragAmount
    dragTarget = tabFractionAtPosition(dragPosition.x, with(density) { 4.dp.toPx() }, tabWidth, tabsCount, isLtr)
    updateValue(dragTarget)
    animationScope.launch {
        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
    }
}
```
**为什么**：每帧用手指绝对坐标算小数 tab 索引（非整数），`updateValue` 弹向小数目标，滑块连续跟随手指、可停在两 tab 之间。`dragPosition += dragAmount` 复用既有绝对坐标累积逻辑（`onDrag` 只收到 `dragAmount`，无绝对坐标）。

### 4. `onDragStopped` 无需改动
[:127-139](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L127-L139) 已为 `dragTarget.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)`。改动后 `dragTarget` 为小数，`fastRoundToInt()` 即「四舍五入到最近 tab」——正好实现用户选的「松手吸附到最近 tab」。`onDragCancelled`（[:123-126](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L123-L126)）回退到 `selectedNow()` 也保持不变。

### 5. 补充单元测试
文件：`app/src/test/kotlin/com/banqiu/thirdparty123pan/DownloadAndDockTest.kt`，新增对 `tabFractionAtPosition` 的断言（小数连续性 + RTL 镜像 + 越界钳制），并保留现有 `tabIndexAtPosition` 用例不动：
```kotlin
@Test fun fractionTracksFingerContinuously() {
    assertEquals(0.5f, tabFractionAtPosition(4f + 50f * 0.5f, 4f, 50f, 4, true), 1e-3f)
    assertEquals(1.5f, tabFractionAtPosition(4f + 50f * 1.5f, 4f, 50f, 4, true), 1e-3f)
    assertEquals(2.5f, tabFractionAtPosition(4f + 50f * 2.5f, 4f, 50f, 4, true), 1e-3f)
}
@Test fun fractionClampsToRange() {
    assertEquals(0f, tabFractionAtPosition(-100f, 4f, 50f, 4, true), 1e-3f)
    assertEquals(3f, tabFractionAtPosition(300f, 4f, 50f, 4, true), 1e-3f)
}
@Test fun fractionMirrorsForRtl() {
    // RTL：最左 x → count-1，向右递减
    assertEquals(3f, tabFractionAtPosition(4f, 4f, 50f, 4, false), 1e-3f)
    assertEquals(0f, tabFractionAtPosition(4f + 50f * 3f, 4f, 50f, 4, false), 1e-3f)
}
```
导入：在测试顶部 import `tabFractionAtPosition`。

## 假设与决策
- 期望行为（用户已确认）：拖动连续跟随手指，松手吸附到最近 tab。
- 交付范围（用户已确认）：根因分析 + 实现修复（改代码）。
- 保留 `tabIndexAtPosition` 不删（仍被 3 个测试覆盖，语义有效，便于复用），仅新增 `tabFractionAtPosition`。
- 不改动 `DampedDragAnimation`、`InteractiveHighlight`、`GlassDock`、`third_party/` 参考——根因与修复都集中在适配副本的拖动回调。
- 松手吸附逻辑沿用既有 `fastRoundToInt()`（最近 tab），不引入额外阈值/死区。
- `consumeGestures = true`（整面板 Initial-pass 观察者，[:117](file:///c:/Users/冷夜/.trae-cn/worktrees/ThirdParty123pan/fix-dock-glass-slider-snap-issue-eDbvQ3/app/src/main/kotlin/com/banqiu/thirdparty123pan/ui/components/upstream/components/LiquidBottomTabs.kt#L117)）保持不变，tap-to-select 语义经小数+取整仍成立。

## 验证步骤
1. 编译：`gradlew.bat :app:compileDebugKotlin`（Windows）。
2. 单测：`gradlew.bat :app:testDebugUnitTest`（或 `python scripts/run_junit_windows.py`，需先由 gradle 生成 `app/build/unit-test-classpath.txt`）。确认新增 3 个 `fraction*` 用例与原 `tabIndexAtPosition` 用例全部通过。
3. 装 APK 手测：`gradlew.bat :app:assembleDebug` 后安装。拖动 dock 玻璃滑块——应连续跟随手指（可停在两 tab 之间），松手后弹簧式吸附到最近 tab 并切换页面。
4. 回归：确认轻点任一 tab 仍能选中该 tab（tap-to-select 未坏）；RTL 布局下方向正确。
