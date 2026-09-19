# 结构采掘器三页面设计方案

> 目标：把 `MythicMinerWorkPage` / `MythicMinerInfoPage` / `MythicMinerAttributesPage` 三个空壳填成完整页面。
> 所有坐标均为**页面局部坐标**（`leftPos`/`topPos` 平移之后的坐标系），与 `MythicMinerScreen.drawPage` 的内层 pose 一致。
>
> 标注约定：
> - **【实测】** 已用字节码 / 像素采样 / 读测试源码验证的事实，附证据来源。
> - **【决策】** 设计选择，附理由。
> - **【已定】** 汐阳已拍板的决策，不再讨论。
>
> **本轮定稿的两项决策**：
> 1. `MythicMinerLayout` 旧几何 → **成组重写**（`MythicMinerLayout` + `MythicMinerGeometry` + `MythicMinerLayoutTest`）。
> 2. 线程槽位/选择器 → **20 × 20 + 数字版本，stride 22**。
>
> **实现状态（2026-09-17）**
> **三页已全部落地**，采用的变体组合是 **W2 + I2 + A3**（见 §7.4）：
> - 栅格 §0.1/§0.2 → `MythicMinerInfoLayout`（含 W2 仪表行、I2 视口、A3 双列常量）
> - 进度条裁剪 §1.3 → `MythicMinerProgressStrip` + `MythicMinerSpriteRenderer.progressStrip`
> - 成组重写 §0.7 → `MythicMinerLayout` 精简、`MythicMinerGeometry` 删除、两个测试重写
> - 三页渲染 → `MythicMinerWorkPage` / `MythicMinerInfoPage` / `MythicMinerAttributesPage` 全部填充
> - 交互接线 → `MythicMinerPageRenderer` 新增 `mouseClicked` / `mouseScrolled` 分发；
>   `MythicMinerScreenContext` 新增 `hoveredMarkerSlot` / `markerAnalysisReady` / `clickContainerSlot`
> - **槽位与按钮全部改用精灵图**：`marker(g,x,y,selected)` 用 `(0,80)`/`(18,80)` 两个变体，
>   选择器用 `smallButton(g,x,y,hovered,pressed)` 的 `(0,98)`/`(20,98)`/`(0,118)` 三态
> - lang 补齐 5 个新 key + 修改 `marker_info.select` 文案（provider 与 generated 双改）
>
> ⚠️ **验证程度**：纯类 `javac` 编译通过；栅格算术独立复算 22 项全绿；lang key 已核。
> 凡是**调用 Minecraft / Forge API** 的类（三页、Screen、Menu）**未经编译** —— 本机 javac 通道
> 对 MC 方法调用一律报"找不到符号"（该 jar 是 SRG 名）。需汐阳本地跑 `compileJava` 与 JUnit。

---

## 0. 共同约束

### 0.1 画布：208 × 123，已被测试锁定

```java
// MythicMinerScreen
private static final int INFO_X = 32, INFO_Y = 33, INFO_W = 208, INFO_H = 123;
graphics.enableScissor(toScreenX(INFO_X), toScreenY(INFO_Y),
                       toScreenX(INFO_X + INFO_W), toScreenY(INFO_Y + INFO_H));
```

**【实测】** `MythicMinerInfoLayoutTest#viewportAndMarkerGridStayInsideInformationArea` 直接断言：

```java
assertEquals(208, MythicMinerInfoLayout.INFO_W);
assertEquals(123, MythicMinerInfoLayout.INFO_H);
for (int i = 0; i < 9; i++)
    assertTrue(MythicMinerInfoLayout.MARKER_X + i * MythicMinerInfoLayout.MARKER_STRIDE + 18 <= 240);
assertTrue(MythicMinerInfoLayout.MARKER_Y + 18 <= 156);
assertTrue(MythicMinerInfoLayout.INFO_LIST_Y + MythicMinerInfoLayout.INFO_LIST_H <= 156);
assertTrue(MythicMinerInfoLayout.ATTR_LIST_Y + MythicMinerInfoLayout.ATTR_LIST_H <= 156);
```

这组断言锁定了三件事，**它们是你这次需求的技术底座**：

1. 画布 = 局部 `x ∈ [32, 240)`、`y ∈ [33, 156)`，即 **208 × 123**。
2. **标记器单行、最多 9 个**（`for i < 9`）。
3. 三个页面的列表/视口都必须收在 `y ≤ 156` 内。

（原断言用字面量 `18` 表示格子边长；改成 20 后该字面量要换成 `MARKER_SIZE`，见 §5。）

### 0.2 槽位栅格：20 × 20，stride 22，起点 38

**【已定】** 20 × 20 格子、`stride 22`、带区居中于内容区。

```java
static int laneX(int i, int n) {
    int laneWidth = MARKER_SIZE + MARKER_STRIDE * (n - 1);   // 20 + 22*(n-1)
    return CONTENT_X + (CONTENT_W - laneWidth) / 2 + i * MARKER_STRIDE;
}
```

| n | 带宽 | 起点 | 终点 | 右侧留白 |
|---|---|---|---|---|
| 1 | 20 | 126 | 146 | 90 |
| 3 | 64 | 104 | 168 | 68 |
| 5 | 108 | 82 | 190 | 46 |
| 9 | 196 | **38** | **234** | 2 |

校验：`38 + 8 × 22 + 20 = 234 ≤ 236`（内容右界）✓，也 `≤ 240`（画布右界）✓
→ **"9 格正好贴合 200 宽"成立**：196 / 200 = 98%，两侧各留 2px。

**【决策】`MARKER_X` 直接定为 38**（居中后的实际起点），而不是 36。这样 `MARKER_X + i * MARKER_STRIDE + MARKER_SIZE` 就是可以直接写进测试的越界公式，不需要在断言里再补一个 `+ 2` 的居中偏移。

**【决策】工作页槽位与信息页选择器共用同一套 20 × 20 栅格。** 两页切换时第 i 格永远在同一条竖线上 —— 这是跨页一致性最值钱的地方。

**【实测】** `MythicMinerSpriteRenderer.marker()` 是 **18×18** 槽位底图素材（`MARKER_U/V = 0/80`，`MARKER_W/H = 18/18`），且**目前全项目零引用**。

**【决策】20×20 格子内，把 18×18 的 `marker()` 居中画在 `(x + 1, y + 1)`；16×16 的物品图标居中画在 `(x + 2, y + 2)`。**

理由：18 → 20 不是整数倍，**绝不能拉伸**（像素美术一旦非整数缩放就会糊）。居中留 1px 外扩既保住了素材原样，又让 20×20 的格子占位与信息页数字按钮严格对齐。物品 16×16 落在 `marker()` 的 18×18 内区（各留 1px 内边距），层次正好。

`GuiChrome.slotFace`（16×16 程序化槽井）**不再用于槽位带** —— 它在 20×20 网格里会留 4px 缝。

### 0.3 坐标系规则

| 调用 | 用哪个坐标 |
|---|---|
| `blit` / `fill` / `drawString` / `renderItem` | **局部** |
| `enableScissor` | **绝对**，必须换算 |

页内滚动要开内层 scissor，**必须**用现成的 `MythicMinerLayout.scaleToScreen(leftPos + x, topPos + y, w, h, uiScale)`（已算进 `uiScale`，且被 `MythicMinerLayoutTest#scissorBoundsIncludeGuiOriginAndCustomScale` 锁定）。不要自己写 `leftPos + x`。

### 0.4 绘制顺序：两个已实测的坑，必须先修

**坑 1：菜单槽位在屏幕外，仍会被画出来。**

`MythicMinerMenu.addContainerSlots()` 把 9 个标记器槽位全部建在 `(-100, -100)`。但 `AbstractContainerScreen` 会遍历所有槽位并对 `isActive()` 为真者调 `renderSlot`。

**【实测】** 反编译 `net.minecraft.client.gui.screens.inventory.AbstractContainerScreen#m_88315_`（render）字节码：

```
 62: iload 7                      // 循环下标
 64: menu.f_38839_.size()         // slots.size()
 74: if_icmpge 175
 94: slot.m_6659_()               // Slot.isActive()
 99: ifeq 109                     // 为假则跳过
106: invokevirtual m_280092_      // → renderSlot(GuiGraphics, Slot)
```

`SlotItemHandler` 未重写 `isActive()`（恒为 true），所以 9 个图标会被画在 `(leftPos - 100, topPos - 100)`。以 854×480 窗口为例 `leftPos ≈ 314, topPos ≈ 113` → 绝对坐标约 `(214, 13)`，**在屏幕内、面板左上方，肉眼可见**。

**坑 2：页面的二次绘制会盖住槽位物品。**

`MythicMinerScreen.render` 在 `super.render` **之后**再画一遍页面（作者注释："Paint the page a second time on top so slot/background passes can never obscure information text"）。页面里的 `blit`/`fill` 一定晚于 `renderSlot`，**任何不透明内容都会盖掉 vanilla 画的槽位物品**。

**【决策】标记器槽位采用「隐形化 + 页面全权自绘 + 手动转发交互」：**

1. `addContainerSlots` 坐标从 `(-100, -100)` 改为 `(-2000, -2000)`，彻底移出可绘制区域（保留 `isActive() == true`，因为 `slotClicked` 链路仍需要它）。
2. 槽位底图与物品由页面自己画：`MythicMinerSpriteRenderer.marker(g, x + 1, y + 1)` + `graphics.renderItem(stack, x + 2, y + 2)`。
3. 命中测试由页面负责，命中后经 `Context` 转发到 `AbstractContainerScreen.slotClicked(slot, index, button, ClickType.PICKUP)`，从而白送**拾取 / 放入 / 拖拽 / shift 快速移动**的完整语义。
4. 物品 tooltip 由页面自建（屏幕绝对坐标调 `graphics.renderTooltip`），不复用 `hoveredSlot`（它在 `-2000` 处永远不命中）。

理由：与项目现有的 `drawExternalButtons` + `handleExternalButtonClick`（自绘按钮 + 手动转发 `handleInventoryButtonClick`）完全同构。

### 0.5 数据取样原则

**每帧只取一次 `MythicMinerTelemetrySnapshot`，全程复用。**

```java
MythicMinerTelemetrySnapshot t = context.menu().telemetrySnapshot();
```

`telemetrySnapshot()` 构造时会遍历全部槽位做 6 次 `combineWord`；逐字段调 `menu.getMarkerXxx(slot)`（旧版 HEAD 的做法）等于把这份成本乘上访问次数，而且会让同一屏内不同行的读数来自不同次采样 —— 出现「自然 30% / 实际 90%」这种自相矛盾的画面。

`MythicMinerAnalysisSnapshot`（维度价值 / 结构价值 / 期望物品）是异步回包的，**必须允许为空**：为空时显示 `marker_info.loading`，不能显示 0。

### 0.6 调色板与文字归属

| 承载面 | 可用文字 token |
|---|---|
| 浅色 `FACE` (`0xFFC6C6C6`) | `INK` / `DIM`（都是纯黑）+ `AMBER` / `FLUIX` / `SUCCESS` / `ERROR` |
| 深色 `WELL` (`0xFF808080`) | `TEXT` / `MUTED`（浅色） |
| 深色 `BAND` (`0xFF2B2B2B`) | `TEXT` / `MUTED` + `WELL_*` |

**不要混用**：`TEXT`/`MUTED` 是浅色，只给深色面；`INK`/`DIM` 是纯黑，只给浅色面。`GuiChromeTest` 断言每个文字 token 在其承载面上的对比度 ≥ 3.0。

数据轴的语义分工（三页共用）：

| 轴 | 颜色 | 用途 |
|---|---|---|
| 结构数据 | `FLUIX` | 结构名、维度、期望值、选择态 |
| 时间 / 收益窗口 | `AMBER` | 进度、tick、升级加成、等待窗口 |
| 成功 / 已启用 | `SUCCESS` | 结构完整、已启用、属性达标 |
| 阻塞 / 已停用 | `ERROR` | 结构缺失、槽位停用、期望物品被禁用 |

---

## 0.7 【已定】单行布局与 `MythicMinerLayoutTest` 正面冲突 → 成组重写

**【实测】** `MythicMinerLayoutTest` 有 9 个测试方法，**全部锁定 `MythicMinerLayout` 的三列网格几何**：

| 测试 | 断言 | 与「单行 20×20」的矛盾 |
|---|---|---|
| `usesRequestedMarkerSlotGrids` | `columnsForSlotCount(9) == 3`、`rowsForSlotCount(9) == 3` | 要求 3 列 3 行；单行是 9 列 1 行 |
| `markerBayKeepsThreeRowHeightAndContainsEverySlot` | `markerBayWidth(*) == 80`、`markerBayHeight(*) == 94` | 单行带宽 196、高 20 |
| `progressBarAlignsWithSlotFrameAndDoesNotOverlapNextRow` | `PROGRESS_WIDTH == 18`、`PROGRESS_HEIGHT == 3`、`progressBarX(x) == x - 1` | 新进度条是 **16 × 4**、`x + 2`（16 宽居中于 20） |
| `attributesFollowTheFixedMarkerSection` | `attributeY(*) == 142` | 单行布局下该带区不存在 |
| `playerInventoryStaysBelowTheFixedMarkerSection` | `playerInventoryY(*) == 258` | **与槽位几何无关，必须原样保留** |
| `scissorBoundsIncludeGuiOriginAndCustomScale` | `scaleToScreen` 换算 | **与槽位几何无关，必须原样保留** |
| 两个 `namedGeometry*` | `MythicMinerGeometry` 的 `markerBay` / `overview` / `operations` | 需随 `MythicMinerLayout` 一起重写 |

删掉 `MythicMinerLayout` 的槽位几何会让 `MythicMinerLayoutTest` 直接编译失败 —— 这不是「顺手删常量」能过的改动。

**【已定】成组重写三个文件：**

1. **`MythicMinerLayout`** —— 删除：
   `MARKER_BAY_X/Y`、`MARKER_SLOT_X_OFFSET`、`MARKER_COLUMN_STRIDE`、`MARKER_ROW_STRIDE`、`PROGRESS_X_OFFSET/Y_OFFSET/WIDTH/HEIGHT`、`columnsForSlotCount`、`rowsForSlotCount`、`markerSlotX/Y`、`progressBarX/Y`、`markerBayWidth/Height`、`markerInfoX/Width/Height`、`attributeY`、`FIXED_MARKER_BAY_*`、`ATTRIBUTE_GAP`、`BASE_MARKER_BAY_HEIGHT`、`MARKER_BAY_ROW_HEIGHT`、`MARKER_BAY_HORIZONTAL_PADDING`。
   **保留**：`playerInventoryY`（测试锁定 258）、`scaleToScreen` + `ScissorBounds`（测试锁定换算）、`hasFluidInput`、`FLUID_*` 系列、`BASE_PLAYER_INVENTORY_Y`。
2. **`MythicMinerGeometry`** —— 只被 `MythicMinerScreen.drawFluidModule`（死代码）引用，改写风险低。
   保留 `fluidPanel` / `inventory` / `tabs` 三个仍有意义的 Rect，删除描述已消失的三列网格的 `markerBay` / `overview` / `operations`。
3. **`MythicMinerLayoutTest`** —— 把「三列网格」断言换成分行约束；保留标了「必须原样保留」的两条；`namedGeometryKeepsFluidAndInventoryOutsideWorkPanels` 的液面/背包不相交断言保留，`namedGeometryMatchesRenderedTabAndWorkBands` 随 `MythicMinerGeometry` 删除。

**收益**：项目里只留一套槽位几何。若不重写而保留旧常量为测试和 `MythicMinerGeometry` 续命，会同时存在两套槽位几何、其中一套永远不绘制 —— 典型的两个真相来源。

---

## 1. 工作页面（WORK）

**职责**：一屏回答「机器现在在干什么、每个线程到哪了、整机产出能力如何」。

### 1.1 布局栅格

```
y=37   ┌ 槽位带：单行 N 格 × 20×20，stride 22，居中 ────┐  20px
y=57   │ 进度条：每格正下方，16×4，0px 间隙             │   4px
y=66   │ ┌ 效率 ────┐ ┌ 幸运 ────┐                    │
y=97   │ ┌ 基础并行 ┐ ┌ 等效加速 ┐                    │  27px×2
y=136  │ [输出模式] [红石] [结构]                       │  12px
y=156  └───────────────────────────────────────────────┘
```

| 区块 | 矩形 | 内容 |
|---|---|---|
| 槽位带 | `y = MARKER_Y = 37`，高 20 | 见 §1.2 |
| 进度条 | `y = MARKER_Y + MARKER_SIZE = 57`，高 4 | 见 §1.3 |
| 卡 1（效率） | `(36, 66, 98, 27)` | `attribute.efficiency` |
| 卡 2（幸运） | `(138, 66, 98, 27)` | `attribute.luck` |
| 卡 3（基础并行） | `(36, 97, 98, 27)` | `attribute.parallel` |
| 卡 4（等效加速） | `(138, 97, 98, 27)` | `overview.equivalent_acceleration` |
| 状态条 | `(36, 136, 200, 12)` | 3 个 chip |

列宽校验：`36 + 98 = 134`，`+4 = 138`，`138 + 98 = 236` ✓ 与内容右边界精确贴合，不留零头。

**【决策】不设独立的标题行。** 画布只有 123px 高，槽位带顶到 `y = 37`（画布顶 + 4）是既有常量的设计意图；再挤 8px 放标题会把数据区压到排不下两行卡片。线程数与总并行改由卡片承载。

### 1.2 槽位带

单行，最多 9 格，每格 **20 × 20**，步长 22（见 §0.2 的居中公式）。

每格绘制顺序（自下而上）：

1. **底图** —— `MythicMinerSpriteRenderer.marker(g, x + 1, y + 1)`（18×18 原生素材居中）
2. **物品** —— `g.renderItem(slot.getItem(), x + 2, y + 2)`（16×16 居中）
3. **空态** —— 空槽画中央 1px 十字（`SLOT_HILIGHT`），暗示可放入
4. **状态叠加**

   | 状态 | 叠加 |
   |---|---|
   | 选中 | 格子顶边 1px `FLUIX`：`fill(x, y, x + 20, y + 1, FLUIX)` |
   | 悬停 | 格子整体叠 `HOVER`（`0xFFDCDCDC` 压到 ~`0x66` alpha） |
   | 已停用 | 格子整体叠 `DISABLED_OVERLAY` |

5. **状态灯** —— 格子内右上角 `(x + 15, y + 1)`，3×3 实心方块：

   | 线程状态 | 颜色 | 判定 |
   |---|---|---|
   | 运行中 | `FLUIX` | `processingTime > 0 && enabled` |
   | 等待自然窗口 | `AMBER` | `waitingForNaturalWindow` |
   | 已停用 | `ERROR` | 已配置且 `!enabled` |
   | 空槽 / 未配置 | 不画 | 无物品，或物品不是结构标记器 |

   3×3 放在右上角，不压标记器图标主体（图标重心在中央 10×10，位置 `(x+2, y+2)` 起）。

**点击语义【决策】**：统一走 vanilla 语义 —— 左键 → `slotClicked(PICKUP)`，手上拿着标记器就放入、空手就取出；同时无论物品是否变化，都调 `context.selectMarkerSlot(index)` 让信息页跟上。玩家不需要学两套操作。

### 1.3 进度条（精灵图裁剪渲染）

**素材已逐像素采样确认【实测】**（`assets/dimension_tech/guis/spritesheet.png`，160×160 RGBA）：

| 区域 | 采样颜色 | 语义 |
|---|---|---|
| `(80,0)-(95,3)` | `373B72` / `915DCD` / `B06FDD` / `6054A6` / `FF80D7` | **充能条**（紫色能量芯） |
| `(96,0)-(111,3)` | `413F54` / `4D4D67` / `696D88` / `878FA5` | **空槽轨**（冷灰） |

判定依据：`915DCD` / `B06FDD` / `FF80D7` / `6054A6` 与 `GuiPalette` 的 `ENERGY_FILL_LIGHT` / `ENERGY_FILL_BRIGHT` / `ENERGY_FILL_HOT` / `ENERGY_FILL_MID` **逐字节相等**；`4D4D67` 与 `ENERGY_BASE_MID` **逐字节相等**。这两块不是通用装饰条，而是 AE2 风格能量仪表的「满 / 空」配对素材：2px 周期竖条纹 + 中间行高光，相位在 x=0 处对齐。

**渲染方式【决策】：用 `blit` 的宽度参数做左→右裁剪，不做任何缩放。**

`GuiGraphics.blit(ResourceLocation, x, y, blitOffset, u, v, width, height, texW, texH)` 里的 `width` **同时**决定「从 `u` 起截取多少纹理像素」和「在屏幕上占多少像素」。因此：

```java
// 1) 空轨：整条铺满
g.blit(SPRITESHEET, barX, barY, 0, 96, 0, 16, 4, TEXTURE_SIZE, TEXTURE_SIZE);

// 2) 充能：从左向右覆盖，只取前 filled 列纹理
int filled = MythicMinerProgressStrip.pixels(progress, processingTime, 16);
if (filled > 0) {
    g.blit(SPRITESHEET, barX, barY, 0, 80, 0, filled, 4, TEXTURE_SIZE, TEXTURE_SIZE);
}
```

这天然就是需求描述的「按当前进度从左向右覆盖」，而且因为**没有缩放**，竖条纹纹理不会被拉伸变形 —— 这是像素美术的硬要求。

**绝对不要用 `MythicMinerTheme.progress()` / `GuiChrome.progress()`**：那是 `width * value / max` 的**缩放**绘制 + 6px 周期 notch 图案、高 3px。用它会（a）把 16×4 的素材拉伸成非原样，（b）高度与素材不符，（c）叠出一层与精灵图完全无关的 tick 图案。

**位置**：`barX = slotX + 2`（16 宽居中于 20 宽格子），`barY = MARKER_Y + MARKER_SIZE = 57`。

**【决策】把 `MARKER_PROGRESS_Y` 改成推导式 `MARKER_Y + MARKER_SIZE`。** 需求是「槽位下方 0 像素」，槽位占 `37..56`，下一像素行就是 57。原常量 56 会留缝 —— 让它成为公式而不是一个需要手动维持的数字。

进度换算（纯函数，单独成类以便单测）：

```java
static int pixels(long progress, long processingTime, int width) {
    if (processingTime <= 0L || progress <= 0L || width <= 0) return 0;
    return (int) Math.min(width, Math.min(progress, processingTime) * width / processingTime);
}
```

`progress` 来自 `menu.getMarkerProgress(slot)`（逻辑进度，跨周期不清零），**必须** `min(progress, processingTime)` 夹一下，否则超过一个周期后会算出越界宽度。

**状态映射（不改变素材，只叠色 / 侧标）**：

| 状态 | 表现 |
|---|---|
| 空槽 / 未配置 | 只画 1px 底线 `PROGRESS_TRACK`，表示「无线程」 |
| 已配置 + 已停用 | 空轨 + 整条 1px 红边（`ERROR`），不画充能 |
| 运行中 | 空轨 + 紫色充能 |
| 等待自然窗口 | 空轨 + 紫色充能 + 条右侧 2×4 的 `AMBER` 竖标记（提示「已被限速」） |

【决策】不加数字刻度 —— 16px 宽度放不下，精度由悬停 tooltip 承担。

### 1.4 汇总数据卡

四张卡，用现成原语 `MythicMinerTheme.metricCard(g, x, y, w, h, accent)`（浅色面 + 左侧 2px accent rail）。

| 卡 | 图标 | 标签 key | 值 | accent |
|---|---|---|---|---|
| 效率 | `Items.REDSTONE` | `attribute.efficiency` | `formatDecimal(t.efficiencyHundredths())` | `FLUIX` |
| 幸运 | `Items.RABBIT_FOOT` | `attribute.luck` | `formatDecimal(t.luckHundredths())` | `FLUIX` |
| 基础并行 | `Items.ARROW` | `attribute.parallel` | `t.baseParallel()` | `FLUIX` |
| 等效加速 | `Items.NETHER_STAR` | `overview.equivalent_acceleration` | `menu.getExternalEquivalentAccelerationTicks()` | `AMBER` |

卡内排版（27px 高）：

```
y+4    [图标 16×16]  标签（INK，截断到 w - 26）
y+15                 值（accent 色，x + 22 起）
```

图标画在 `(x + 3, y + 4)`。标签一律 `font.plainSubstrByWidth(label, w - 26)` 截断，绝不溢出卡片。

**【决策】「等效加速」用 `AMBER` 而非 `FLUIX`** —— 它是时间轴数据，与 §0.6 的轴分工一致，也让四张卡里唯一的非结构数据立刻被识别出来。

**【决策】** 不在卡里显示 `%` 符号。`formatDecimal` 已给两位小数，单位靠标签表达，避免 `12.00%` 这种挤在 98px 里的字符串。

### 1.5 状态条

一行三个 chip，`MythicMinerTheme.statusChip`（深底 + 左侧 2px accent + 浅色文字），每个 `(x, 136, 64, 12)`，间距 4：`36 / 104 / 172`，`172 + 64 = 236` ✓。

| chip | 文案 | accent |
|---|---|---|
| 输出模式 | `output.me_network` / `output.item_handler` / `output.none` | `SUCCESS` / `FLUIX` / `ERROR` |
| 红石 | `redstone.*` | 不限速 → `SUCCESS`；限速中 → `AMBER` |
| 结构 | `structure.complete` / `structure_incomplete` | `SUCCESS` / `ERROR` |

**【决策】能量与流体不在这行重复** —— 它们已有左侧竖条 + 悬停 tooltip 的专用通道（`drawEnergyRail` / `drawFluidGauge`），页面里再放一遍只会稀释信号。

结构不完整时，除 chip 变红外，还要在画布底部（`y = 150`）叠一行 `structure_incomplete`（`INK` + 半透明浅色底条）。当前该提示画在 `topPos + 46`，正落在槽位带上，会被槽位压住。

### 1.6 悬停与点击

**悬停槽位**

```
槽位 N · <结构名>          ← FLUIX（未配置时为 marker_info.unconfigured 新增 key）
坐标：X, Y, Z              ← DIM
<启用 / 停用提示>           ← marker_progress_toggle
（空槽时替换为「放入结构标记器」）
```

物品图标 tooltip 用 `graphics.renderTooltip(font, stack, mouseX, mouseY)`，传**屏幕绝对坐标**（`mouseX` 原值，不是局部值）。

**悬停进度条** —— 需求明确要求的「线程进度信息 + 总并行信息」：

```
进度：1234 / 2000 tick        ← marker_progress          INK
本周期自然 tick：1234         ← marker_info.natural_ticks
实际 tick：2468               ← marker_info.actual_ticks（仅外部加速激活时出现）
总并行：12.00                 ← marker_info.parallel      FLUIX
  ├ 基础并行：4
  ├ 额外效率并行：2
  └ 外部加速并行：6.00
等待自然 tick 窗口（400 tick） ← waiting_for_natural_window（仅该状态出现）
──
点击切换启用 / 停用
```

**【决策】总并行在 tooltip 里直接展开三层拆解，不做「点击展开」。** tooltip 是零成本的渐进披露通道，比再做一个折叠控件省得多，也符合「悬停即得答案」。

**点击**

| 目标 | 行为 |
|---|---|
| 槽位 | `selectMarkerSlot(i)` + 转发 `slotClicked` |
| 进度条 | `ModNetwork.toggleMythicMinerSlot(containerId, i)`（已存在） |

### 1.7 空态与异常态

| 场景 | 表现 |
|---|---|
| 无任何标记器 | 槽位带照常画空槽（不隐藏），卡区显示 `--`，状态条正常。**不要**用「无数据」大字盖住布局 —— 骨架稳定，玩家才知道该往哪放东西 |
| 结构不完整 | 状态 chip 变红 + 底部提示；槽位与卡区照常显示（数据可能来自上一周期） |
| 分析未回包 | 工作页不依赖分析结果，无影响 |

### 1.8 数据接口

现有 menu 已完全满足，**无需扩充**：`telemetrySnapshot()`（含 `markers`）、`getMarkerProgress` / `getMarkerProcessingTime` / `isMarkerSlotEnabled` / `isMarkerWaitingForNaturalWindow` / `getMarkerCurrentNaturalTicks` / `getMarkerCurrentExternalAccelerationMachineTicks` / `getWorkingThreadCount` / `getTotalParallel` / `getBaseParallel` / `getMarkerTotalParallel` / `getMarkerExtraEfficiencyParallel` / `getMarkerExternalAccelerationParallelHundredths` / `getExternalEquivalentAccelerationTicks` / `getEfficiencyHundredths` / `getLuckHundredths`。

Context 只需要新增「转发槽位点击」，见 §4.2。

---

## 2. 信息页面（INFO）

**职责**：一屏回答「这一个线程具体在做什么、它的结构值多少、会产出什么」。

### 2.1 布局栅格

```
y=37   ┌ 线程选择器：单行 N 格 × 20×20，数字，stride 22 ─┐  20px
y=57   │ 选中指示条：1px FLUIX，仅选中格下方            │   1px
y=62   │ 摘要：线程 n/N · 结构名            总并行 X     │  13px
y=76   ├ ┌ 滚动视口 190 × 80 ┐ ┌滚┐                     │
       │ │ §1 结构标识        │ │动│                     │
       │ │ §2 工作状况（双条）│ │条│                     │
       │ │ §3 产物期望        │ └─┘                     │
y=156  └─────────────────────────────────────────────────┘
```

| 区块 | 矩形 |
|---|---|
| 选择器 | `y = MARKER_Y = 37`，高 20，几何同 §0.2 |
| 选中指示条 | `(laneX(sel), 57, 20, 1)`，`FLUIX` |
| 摘要行 | `(36, 62, 200, 13)` |
| 滚动视口 | `(36, 76, 190, 80)`（沿用 `INFO_LIST_X/Y/W/H`） |
| 滚动条 | `x = 36 + 190 = 226`，`y = 76`，`height = 80` |

### 2.2 线程选择器（20 × 20 + 数字）

**【已定】20 × 20 格子，格内居中画序号 `1..9`。**

直接复用现成原语：

```java
int accent = selected ? MythicMinerTheme.FLUIX : MythicMinerTheme.EDGE;
MythicMinerTheme.button(g, font, x, y, 20, 20,
                        Component.literal(Integer.toString(index + 1)),
                        hovered, true, accent);
```

`GuiChrome.button` 本来就是「黑边 + 面 + accent 顶条 + 高光 + 阴影 + 居中文字」的 20px 级控件，在 20×20 上正好合适；数字自动居中，不需要自己算 `font.width`。

状态补充：

| 状态 | 表现 |
|---|---|
| 选中 | `accent = FLUIX` + 格子下方 1px `FLUIX` 指示条 |
| 有物品 | 格子右下角 3×3 状态灯（与工作页同色规则，见 §1.2） |
| 空槽 | 不加状态灯，`accent = EDGE` |

**【决策】格内画数字而不是物品图标** —— 玩家的心智能瞬间映射到「第几个线程」，而 9 个相同的结构标记器图标完全无法区分。这正是选 20×20 的收益。

**【决策】序号用 `Component.literal` 而非 `translatable`** —— 数字是语言无关的，不需要进 lang 表。

### 2.3 分节滚动骨架

三个分节，**默认全部展开**，标题条可点击折叠（复用 `markerPropertiesExpanded` / `workStatusExpanded` / `productInfoExpanded`）。

**关键工程要求【决策】**：分节高度必须由**唯一一个纯函数**集中计算。

```java
static int contentHeight(Ctx c, MythicMinerTelemetrySnapshot t);
static int sectionY(Ctx c, MythicMinerTelemetrySnapshot t, Section s);
```

禁止旧版 HEAD 那种写法 —— 把偏移量散在 `sectionAt` / `parallelBounds` / `productsHeaderBounds` / `expectedItemsY` / `contentHeight` / `workStatusHeight` **六个互相耦合的函数**里，任何一处改动都会让点击区域与绘制区域静默错位，而且没有任何测试能发现。

滚动条直接调 `MythicMinerTheme.scrollbar(g, 226, 76, 80, contentHeight, 80, scroll)`。滚轮步长 = `MARKER_INFO_ROW_HEIGHT`（20，旧值，保留）。

### 2.4 §1 结构标识

分节头 18px + 内容（行高 13）：

| 行 | 文案 key | 颜色 |
|---|---|---|
| 结构 | `marker_info.structure`（`槽位 %s：%s`） | `INK` |
| 维度 | `marker_info.dimension`（`维度：%s`） | `INK` |
| 坐标 | `marker_info.position`（`坐标：%s, %s, %s`） | `DIM` |
| 维度价值 | `marker_info.dimension_value` | `FLUIX` |
| 结构价值 | `marker_info.structure_value` | `AMBER` |

结构与维度名走 `TranslateHelper.structureName(id)` / `TranslateHelper.dimensionName(id)`。数据源：`StructMarkerItem.getMarkerInfo(stack)` → `MarkerInfo(dimension, position, structure().id())`。

分析未回包时，两行价值显示 `marker_info.loading`（`DIM`）。

### 2.5 §2 工作状况（双进度条 + 并行拆解）

本页的信息核心。

**双进度条**（需求明确要求）：

```
自然 tick   1234 / 2000            ← 文字行（DIM + FLUIX 数值）
[▓▓▓▓▓▓▓▓▓▓░░░░░░]                 ← 条 A：宽 186，高 4，FLUIX

实际 tick   2468 / 2000   (×2.00)  ← 文字行（仅外部加速激活时出现）
[▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓]                 ← 条 B：宽 186，高 4，SUCCESS
```

| 条 | 值 | 分母 | 色 |
|---|---|---|---|
| A 自然 | `t.markers().get(slot).naturalTicks()` | `processingTime` | `FLUIX` |
| B 实际 | `menu.getMarkerActualProgress(slot)` | `processingTime` | `SUCCESS` |

**两条必须来自同一个 `telemetrySnapshot()`**（§0.5）。双进度条放在一起的**全部意义就是对比**，采样时刻不同则对比无意义。

【决策】当外部加速未激活（`actualTicks == naturalTicks`）时，**条 B 与它的文字行整体隐藏**，改为一行「未检测到外部加速」。理由：两条完全重合的进度条不是在传达信息，是在制造噪声 —— 玩家会怀疑哪条坏了。

条 B 右侧的 `(×N)` 用 `menu.getMarkerCurrentCycleExternalEquivalentAcceleration(slot)`，与条 A 同周期，可直接对比。

> **这一页的进度条用 notch 风格而不是精灵图裁剪**：精灵图素材只有 16px 宽，在 186px 上重复平铺会露出接缝与相位问题；`GuiChrome.progress()` 本来就是为任意宽度设计的（`TICK_STRIDE = 6`）。工作页用精灵图是因为那里的宽度**恰好等于素材宽度**（16 = 16）—— 两者是不同场景，不矛盾。

**并行拆解**（需求要求「含加速产生的额外并行」）：

用**一条 186×6 的水平分段条**，而不是三行文字：

```
[▓▓▓▓▓▓|▓▓|▓▓▓]                      ← 按比例分段的 stacked bar
基础 4 │ 效率 +2 │ 加速 +6.00          ← 图例行（12px）
```

| 段 | 值 | 色 |
|---|---|---|
| 基础并行 | `t.baseParallel()` | `FLUIX` |
| 额外效率并行 | `menu.getMarkerExtraEfficiencyParallel(slot)` | `AMBER` |
| 外部加速并行 | `getMarkerExternalAccelerationParallelHundredths(slot) / 100.0` | `SUCCESS` |

分段宽度按 `value / total * 186` 分配，**最后一段吃掉舍入误差**，保证右端精确贴合。

再补一行参考值：`marker_info.previous_cycle_parallel`（上一周期外部加速并行，`getMarkerPreviousExternalAccelerationParallelHundredths`）。**这个 getter 早就存在但旧版 UI 从未使用过** —— 典型的「有数据没出口」。

【决策】分段条 + 图例远强于三行文字列表：玩家的真实问题是「为什么并行涨了」，分段条让「涨在加速那一段」一眼可见，文字列表则要求他做两次减法。

### 2.6 §3 产物期望

| 项 | 规格 |
|---|---|
| 行高 | `MARKER_INFO_ROW_HEIGHT` = 20 |
| 行内容 | `[物品图标 16×16] 名称（左）……… 期望值（右对齐）` |
| 斑马纹 | 奇数行铺 `STRIPE_INK` |
| 禁用行 | `DISABLED_OVERLAY` + 左侧 2px `ERROR` rail + 文字与数值转 `ERROR` + 名称下 1px 删除线 |
| 空态 | `marker_info.no_items`（`DIM`，居中） |
| 点击 | `ModNetwork.toggleMythicMinerExpectedItem(containerId, slot, itemId)` |
| 悬停 | `context.setExpectedHover(stack, disabled)` → 由 screen 出 tooltip |

期望值格式化用现成的 `MythicMinerScreen.formatExpectedValue`：`< 0.0001` 转科学计数法。**否则最稀有的产物会全部显示成 `0.0000`，玩家会以为坏了。**

列表已由 `applyAnalysis` 按期望降序排序，页面不要重排。

### 2.7 未选中线程

`selectedMarkerSlot() < 0` 时：选择器照常画（空态按钮），视口内居中显示 `marker_info.select`。

**【决策】该文案必须改。** 现值为「点击进度条选择标记器」，但新架构下选择器是独立的一行数字按钮，不再是进度条 → 改为「点击上方槽位选择标记器」。改它要**同时**动 `ModZhcnLangProvider` + `ModEnusLangProvider` + `src/generated` 下的 json 三处（只改 provider 不重跑 `runData`，游戏里会直接显示原始 key）。

### 2.8 数据接口

同 §1.8，另需读槽位物品：`StructMarkerItem.getMarkerInfo(menu.slots.get(slot).getItem())`。

---

## 3. 属性页面（ATTRIBUTES）

**职责**：一屏回答「这台机器是什么等级、属性怎么算出来的、装了哪些升级」。

### 3.1 布局栅格

```
y=37   ┌ 头部 chip 行：[T6] [结构完整] [设备拆解] ───────┐  12px
y=55   ├ ┌ 滚动视口 190 × 101 ┐ ┌滚┐                    │
       │ │ §1 属性总览         │ │动│                    │
       │ │ §2 属性构成         │ │条│                    │
       │ │ §3 已安装升级       │ └─┘                    │
y=156  └─────────────────────────────────────────────────┘
```

| 区块 | 矩形 |
|---|---|
| 头部 chip 行 | `(36, 37, 200, 12)`，3 个 chip |
| 滚动视口 | `(36, 55, 190, 101)` |
| 滚动条 | `x = 226`，`y = 55`，`height = 101` |

属性页没有槽位带（它展示整机而非单线程），所以视口可以从 55 一路到 156。

### 3.2 头部 chip 行

| chip | 内容 | accent |
|---|---|---|
| 等级 | `Tier N`（由方块 tier 得出） | `FLUIX` |
| 结构 | `structure.complete` / `structure_incomplete` | `SUCCESS` / `ERROR` |
| 设备拆解 | `equipment_dismantling_on` / `_off` | `SUCCESS` / `DIM` |

**【决策】等级放这里而不是 §1 属性里** —— 它是「这台机器是什么」的元信息，不是计算结果；和一堆可被升级改变的数值混在一起会让人误以为它也是可升级项。

设备拆解 chip 只在 `supportsEquipmentDismantling()` 为真时出现；为假时该位置**留空**（不要让另外两个 chip 拉伸重排，位置稳定优先）。

### 3.3 §1 属性总览

5 行，行高 13（非交互）：

| 行 | 有效值 | 加成 | accent |
|---|---|---|---|
| 效率 `attribute.efficiency` | `formatDecimal(getEfficiencyHundredths())` | `getEfficiencyBonusHundredths()` | `FLUIX` |
| 储能 `attribute.capacity` | `compact(getEnergyCapacity()) + " FE"` | `getCapacityBonusHundredths()` | `AMBER` |
| 能耗 `attribute.consumption` | `compact(getEffectiveEnergyConsumption()) + " FE/t"` | `getConsumptionReductionHundredths()`（负向） | `AMBER` |
| 并行 `attribute.parallel` | `compact(getBaseParallel())` | `getParallelBonusHundredths()` | `FLUIX` |
| 幸运 `attribute.luck` | `formatDecimal(getLuckHundredths())` | `getLuckBonusHundredths()` | `FLUIX` |

行内排版：`标签（DIM） …… 有效值（INK） …… 加成（accent，右对齐）`。

- `compact()`：≥1e9 → `B`，≥1e6 → `M`，≥1e3 → `K`。**必须用** —— 否则 `1048576 FE` 会顶到右边界。
- 加成显示 `(+20%)` / `(-15%)` / `(0%)`，用 `BigDecimal.valueOf(v, 2).stripTrailingZeros()` 去尾零。**能耗是降低量，符号必须是负号**（旧版用 `reduction` 布尔处理，保留该设计）。
- 悬停任一行 → tooltip：`attribute.<name>_value` + `attribute.bonus`/`attribute.reduction` + `attribute.upgrade_breakdown`（专精 N 个、聚合 M 个）。

### 3.4 §2 属性构成（需求的「构成方式」）

这是属性页真正有价值的部分：**展示每个属性的来源拆解**，而不是重复 §1 的数字。

```
并行合计            12.00
  ├ 基础并行（等级决定）   4
  ├ 效率升级并行加成      +2        来自 1 个效率升级
  └ 外部加速并行          +6.00

效率合计            1.00
  ├ 等级基础            1.00
  └ 升级加成            +20%       来自 2 个效率升级（T1 ×2）
```

实现口径（全部落到已有 getter）：

| 属性 | 构成项 | 数据源 |
|---|---|---|
| 并行 | 基础 / 效率加成 / 外部加速 | `getBaseParallel` / `getMarkerExtraEfficiencyParallel` / `getExternalAccelerationParallelHundredths` |
| 储能 | 等级基础 / 升级加成 | `getEnergyCapacity` / `getCapacityBonusHundredths` |
| 能耗 | 基础 / 降低量 | `getEffectiveEnergyConsumption` / `getConsumptionReductionHundredths` |
| 效率 | 等级基础 / 升级加成 | `getEfficiencyHundredths` / `getEfficiencyBonusHundredths` |
| 幸运 | 等级基础 / 升级加成 | `getLuckHundredths` / `getLuckBonusHundredths` |

每组一个「标题行（20px，可折叠）+ N 个子行（11px）」，子行缩进 12px 并用 `├ / └` 引导符。**【决策】默认全部折叠** —— 这一页信息密度最高，默认展开会把玩家直接淹没。标题行右侧显示合计值。

**【决策】「并行」一组的加成项要按 `并行升级 + 聚合升级` 分开标注**（`getParallelUpgradeCount()` / `getAggregateUpgradeCount()`）。聚合升级同时影响五个属性，玩家最常搞混的就是「我该再放一个专精还是聚合」。

### 3.5 §3 已安装升级

遍历顺序固定：`EFFICIENCY, ENERGY, PARALLEL, LUCK, AGGREGATE`；每类 `tier 1..6`，`getUpgradeCount(type, tier) > 0` 才显示。

行高 20（可交互），展开后 `26 + lines × 12`：

```
[图标16]  <升级方块名>  ×3                          [-]
  效率提升 +20%              ← 展开行，accent 着色
  ──────────────────────────
```

图标映射：`EFFICIENCY→REDSTONE`、`ENERGY→BEACON`、`PARALLEL→ARROW`、`LUCK→RABBIT_FOOT`、`AGGREGATE→NETHER_STAR`。

展开内容来自配置：`ModConfigs.UPGRADE_TIERS[tier - 1]`，聚合用 `AGGREGATE_UPGRADE_TIERS[tier - 1]`，字段 `efficiencyIncreasePercent()` / `energyCapacityIncreasePercent()` / `energyConsumptionReductionPercent()` / `parallelIncreasePercent()` / `luckIncreasePercent()`。

各类型展开行数与颜色：

| Type | 展开行 | 颜色 |
|---|---|---|
| EFFICIENCY | 效率 | `FLUIX` |
| ENERGY | 储能、能耗 | `AMBER` |
| PARALLEL | 并行 | `FLUIX` |
| LUCK | 幸运 | `FLUIX` |
| AGGREGATE | 全部五行 | 各自轴色 |

文案 key 走 `tooltip.dimension_tech.mythic_miner.upgrade.<suffix>`；其中能耗有两个变体 —— 专精用 `energy_consumption.multiplicative`（乘算）、聚合用 `energy_consumption.additive`（加算）。**这个区分承载真实机制差异，必须保留。**

升级方块名 key：`block.dimension_tech.mythic_miner_upgrade_<type.lowercase>`，tier > 1 时追加 `_tier_N`。折叠状态用 `context.upgradeRowExpanded(key)` / `toggleUpgradeRow(key)`，key 沿用 `type.ordinal() * 10 + tier`（已存在，对 `NONE` 也安全 —— `NONE.ordinal() == 0`）。

空态：一个都没装时，视口内显示一行 `attribute.installed` 提示。

### 3.6 悬停与点击

| 目标 | 行为 |
|---|---|
| §1 属性行 | 属性 tooltip |
| §2 组标题行 | 切换折叠 |
| §3 升级行 | 切换折叠（左键） |
| 滚轮 | 在视口内滚动 |

**不提供任何修改操作** —— 属性页是只读的。升级的增减通过多方块结构本身完成，给 GUI 加装/卸按钮会引入第二条真相来源。这条建议写进类注释固化。

---

## 4. 公共设施

### 4.1 抽一个 `MythicMinerSectionList`

INFO 与 ATTRIBUTES 共享同一套骨架：`分节头 + 折叠 + 视口 + 内层 scissor + 滚动条 + 命中测试`。

```java
final class MythicMinerSectionList {
    static int contentHeight(List<Section> sections);
    static int headerY(List<Section> sections, int index, int scroll);
    static int rowAt(List<Section> sections, int scroll, double localX, double localY);
    static boolean scroll(...);
    static void draw(GuiGraphics g, Font font, Rect viewport, List<Section> sections, int scroll);
}
```

不抽的后果已经被旧版 HEAD 演示过（§2.3 的六个耦合高度函数）。

### 4.2 `MythicMinerScreenContext` 需要新增的能力

```java
/** 转发一次真实的槽位点击，走 vanilla 的 PICKUP 语义。 */
void clickContainerSlot(int index, int mouseButton);

/** 三页共用的滚动条 x，避免每页各写一个数字。 */
int infoScrollbarX();     // → MythicMinerInfoLayout.INFO_LIST_X + INFO_LIST_W
```

其余（`leftPos` / `topPos` / `uiScale` / `font` / `menu` / 折叠状态 / 滚动偏移 / 分析结果）都已在接口里，够用。

同时给 `MythicMinerPageRenderer` 补三个分发入口，让交互跟随页面走（与旧版「胖页面」设计一致，但接口收窄）：

```java
static void render(MythicMinerScreenContext c, GuiGraphics g, Page page);
static boolean mouseClicked(MythicMinerScreenContext c, double x, double y, int button);
static boolean mouseScrolled(MythicMinerScreenContext c, double x, double y, double delta);
static void renderTooltip(MythicMinerScreenContext c, GuiGraphics g,
                          int logicalMouseX, int logicalMouseY, int screenX, int screenY);
```

**当前 `MythicMinerScreen.mouseScrolled` 是彻底空转的**（算完局部坐标就直接 `return true`），滚动必须接上这些入口才生效。

### 4.3 清理清单

| 项 | 动作 |
|---|---|
| `MythicMinerLayout` 旧槽位几何 | 删除 + **同步重写 `MythicMinerLayoutTest`**，详见 §0.7 |
| `MythicMinerScreen.INFO_PANEL_Y` / `INFO_SLOT_Y` / `INFO_VIEWPORT_Y` / `MARKER_INFO_PADDING` / `PARALLEL_BREAKDOWN_HEIGHT` / `MARKER_INFO_EXPECTED_Y` | 删除（页面清空后已无引用） |
| `MythicMinerInfoLayout.REDSTONE_BUTTON_INDEX` | 删除（死常量，值 3 只在 Tier 3+ 成立 —— 上一轮 tooltip bug 的同源陷阱） |
| `MythicMinerScreen.drawFluidModule` / `drawFluidButton` / `drawEnergyIcon` | 死代码，删除 |
| `MythicMinerScreen.mouseScrolled` | 接上页面分发 |
| `addContainerSlots` 的 `-100,-100` | 改 `-2000,-2000`（§0.4 坑 1） |
| `MythicMinerSpriteRenderer.marker()` | **从死代码变为槽位底图**（§0.2） |

---

## 5. 验证清单

**必须由汐阳本地执行**（本机 Agent 环境跑不起 `gradlew`）：

1. **`MythicMinerLayoutTest` 重写并全绿** —— 本次最大的回归面，见 §0.7。
2. `compileJava` + `runData`：语言 key 改了/新增了必须重跑 datagen，否则游戏里显示原始 key。
   新增 key：
   - `screen.dimension_tech.mythic_miner.work.threads`（`标记线程 %s/%s`）
   - `screen.dimension_tech.mythic_miner.work.total_parallel`（`总并行 %s`）
   - `screen.dimension_tech.mythic_miner.marker_info.unconfigured`（`未配置结构标记器`）
   - `screen.dimension_tech.mythic_miner.marker_info.insert`（`放入结构标记器`）
   - `screen.dimension_tech.mythic_miner.attribute.composition`（`属性构成`）
   - `screen.dimension_tech.mythic_miner.attribute.source.base`（`等级基础`）
   修改 key：`marker_info.select`（§2.7）。zh_cn / en_us 必须同步。
3. `GuiChromeTest`：新增文字色必须来自对应承载面的 token 集合，对比度断言仍须通过。
4. **新增 `MythicMinerProgressStripTest`**：`pixels()` 在 `progress = 0` / `processingTime = 0` / `progress > processingTime` / `width = 16` 边界的返回值。这是本次唯一能脱离运行时验证的逻辑。
5. **改写 `MythicMinerInfoLayoutTest`**：
   - `MARKER_SIZE == 20`、`MARKER_STRIDE == 22`、`MARKER_X == 38`
   - `MARKER_X + 8 * MARKER_STRIDE + MARKER_SIZE <= 236`（→ `234 ≤ 236` ✓ 内容右界）
   - `MARKER_PROGRESS_Y == MARKER_Y + MARKER_SIZE`（0 间隙公式，**不是硬编码 57**）
   - `MARKER_PROGRESS_W == 16 && MARKER_PROGRESS_H == 4`
6. 游戏内人工验收：
   - 9 个槽位单行居中（两侧各留 2px）、进度条与槽位下沿 0 间隙、条纹纹理未被拉伸
   - 工作页槽位与信息页数字按钮**竖向对齐**（同一列 x）
   - 悬停进度条读到三件套：进度、自然/实际 tick、总并行拆解
   - 停用槽位的整体变暗与红边
   - 信息页三屏内容滚动手感
   - 属性页折叠状态在切页后保留
   - **回归点：面板左上方不应再出现 9 个漂浮的标记器图标**（§0.4 坑 1）

---

## 6. 像素级摘要（实现时对着这张表写）

### 沿用既有值

| 常量 | 值 | 归属 |
|---|---|---|
| `INFO_X` / `INFO_Y` / `INFO_W` / `INFO_H` | 32 / 33 / 208 / 123 | `MythicMinerScreen` + `MythicMinerInfoLayout` |
| `CONTENT_X` / `CONTENT_RIGHT` | 36 / 236 | `MythicMinerInfoLayout` |
| `MARKER_Y` | 37 | 同上 |
| `INFO_LIST_X/Y/W/H` | 36 / 76 / 190 / 80 | 同上 |
| `MARKER_PROGRESS_W` / `_H` | 16 / 4 | 同上 |
| `MARKER_W` / `MARKER_H` | 18 / 18 | `MythicMinerSpriteRenderer`（槽位底图素材） |
| `BAND_HEIGHT` | 14 | `GuiChrome` |

### 本次改动 / 新增

| 常量 | 值 | 归属 |
|---|---|---|
| `CONTENT_W` | 200 | `MythicMinerInfoLayout`（新增，= `CONTENT_RIGHT - CONTENT_X`） |
| `MARKER_SIZE` | **20** | 同上（新增） |
| `MARKER_STRIDE` | **22**（原 18） | 同上 |
| `MARKER_X` | **38**（原 36，居中后起点） | 同上 |
| `MARKER_PROGRESS_Y` | **`MARKER_Y + MARKER_SIZE` = 57**（原硬编码 56） | 同上，写成推导式 |
| `WORK_CARD_ROW_1_Y` / `_ROW_2_Y` | 66 / 97 | 同上（新增） |
| `WORK_CARD_W` / `_H` / `_GAP` | 98 / 27 / 4 | 同上（新增） |
| `WORK_CHIP_W` / `_H` / `_GAP` | 64 / 12 / 4 | 同上（新增） |
| `WORK_STATUS_Y` | 136 | 同上（新增） |
| `INFO_SUMMARY_Y` | 62 | 同上（新增） |
| `ATTR_VIEWPORT` | 36 / 55 / 190 / 101 | 同上（替换 `ATTR_LIST_*`） |
| `SCROLLBAR_X` | 226（= `INFO_LIST_X + INFO_LIST_W`） | 同上（新增） |
| `ROW_H_INTERACTIVE` / `_DATA` | 20 / 13 | 同上（新增） |
| `SECTION_HEADER_H` | 18 | 同上（新增） |
| `PROGRESS_FILL_U` / `_V` | 80 / 0 | `MythicMinerSpriteRenderer`（新增） |
| `PROGRESS_TRACK_U` / `_V` | 96 / 0 | 同上（新增） |
| `PROGRESS_W` / `PROGRESS_H` | 16 / 4 | 同上（新增） |

### 栅格速查

```
y=37  ├ 槽位带 / 数字选择器  20×20，stride 22，起点 38
y=57  ├ 进度条 16×4（工作页，barX = slotX + 2）/ 选中指示条 1px（信息页）
y=62  ├ 信息页摘要行
y=66  ├ 工作页卡行 1
y=76  ├ 信息页视口起点
y=97  ├ 工作页卡行 2
y=136 ├ 工作页状态 chip 行
y=156 └ 画布下界
```

---

## 7. 布局备选方案

§1–§6 是按「W1 卡片矩阵 / I1 折叠分节 / A1 折叠分节」写的。本章给出每页的另外两种排法，
以及把它们混搭时的取舍。

### 7.1 工作页

槽位带（37–56）与进度条（57–60）在三者中完全相同，差异全在 61–156 这 95px。

| | W1 卡片矩阵 | W2 仪表行式 | W3 左右分栏 |
|---|---|---|---|
| 数据区 | 2×2 卡片 `(4,33)/(106,33)/(4,64)/(106,64)`，98×27 | 两列 × 4 行，行高 15，`y=32/47/62/77` | 左栏 5 行 × 13（`y=32..89`） |
| 状态 chip | 底部一行 `y=103`，64×12 ×3 | 同 W1 | 右栏竖排 `y=32/48/64`，98×12 ×3 |
| 可放数据项 | **4 项** | **8 项** | 5 + 2 项 |
| 视觉分块 | 卡片边框 | 左侧 2px accent rail | 中缝 2px 竖线 |
| 底部余量 | 148–156 | 148–156 | 148–156 |

**W1 的实质缺陷**：4 张卡放不下「总并行」，而总并行（`getTotalParallel`）才是实际产能 ——
基础并行只是它的一个加数。W1 只好把基础并行放上去，玩家得去信息页才能看到真实产能。
若保留 W1，应把卡 3 从「基础并行」换成「总并行」，把基础并行降级到 tooltip。

**W2** 用两列 × 4 行塞下：效率 / 幸运 / 基础并行 / 总并行 / 等效加速 / 升级数 / 工作线程 / 能耗。
每项 98px 宽，「标签 + 右对齐数值」绰绰有余。代价是没有卡片边框，区块边界靠 accent rail 与行距暗示 ——
在浅色 `FACE` 上这已经够读，但也确实比 W1 少一层"分组"语义。

**W3** 的不对称是刻意的：左栏放"要盯的数值"，右栏上半放"状态 chip"（输出模式 / 红石 / 结构），
下半放两个次要数值。适合"数值多、状态少"的机器；但左右栏宽度不等会让视觉重心偏左。

新增常量：W2 需 `WORK_METER_ROW_Y{4}` + `WORK_METER_ROW_H(15)`；W3 需 `WORK_SPLIT_X(106)` + 左右栏各自的行走器。

### 7.2 信息页

选择器（37–56）三者相同。差异在"让谁滚动"。

| | I1 全滚动 · 折叠 | I2 全滚动 · 平铺 | I3 固定工作区 + 滚动产物 |
|---|---|---|---|
| 摘要行 | `y=29..41` | **取消**，并入视口首页 | `y=29..41` |
| 视口 | `y=43`，高 80 | `y=29`，**高 94** | 工作区 `y=45` 高 44（**不滚动**）+ 列表视口 `y=93` 高 30 |
| 滚动条 | `(194,43,3,80)` | `(194,29,3,94)` | `(194,93,3,30)` |
| 单屏内容 | 少（靠折叠控制） | 多 ~18% | 工作指标常驻 + 1 行产物 |
| 交互 | 需点击分节头 | 只需滚 | 只需滚 |

**I1 的风险**：折叠状态下每个分节高度都必须由纯函数算出（§2.3），偏移量算错会让点击区与绘制区静默错位。
折叠表达力强，但也把一份脆弱性带进了布局。

**I3 的逻辑**：双进度条是**实时变化**的数据，产物期望是**静态**的。把会变的放进滚动区，
等于玩家滚下去看产物时就看不到进度 —— 而进度恰恰是他回来复刷的理由。
让"会变的常驻、静态的滚动"更贴合这台机器的使用节奏。代价是产物列表只剩 30px（约 1.5 行），
滚动手感会偏紧。

新增常量：I3 需 `INFO_WORK_ZONE_Y/H(45/44)`、`INFO_PRODUCT_VIEWPORT_Y/H(93/30)`。

### 7.3 属性页

| | A1 单视口 · 折叠分节 | A2 固定属性 + 滚动升级 | A3 视口内双列 |
|---|---|---|---|
| 属性总览 | 视口内第一节 | **固定区** `y=20..85`，5 行 × 13 | 视口内左列上段 |
| 属性构成 | 视口内第二节（可折叠） | **并入属性行的展开** | 视口内左列中/下段 |
| 升级列表 | 视口内第三节 | 滚动视口 `y=91` 高 32 | 视口内右列（`x=104`，宽 86） |
| 滚动条 | `(194,22,3,101)` | `(194,91,3,32)` | `(194,22,3,101)` |
| 属性是否常驻 | 否 | **是** | 是（左列上部） |

**A2 的正序性**：属性页的使用节奏是「我现在的数值是多少」（高频）→「为什么是这个值」（中频）
→「装了哪些升级」（低频）。A2 把最高频的钉死，把「构成」作为属性行的展开内容 —— 结果与原因紧邻，
符合因果顺序而非文档顺序。代价是升级列表只剩 32px（约 1.5 行）。

**A3** 一个滚动条覆盖双列，一屏看到全部内容，但左列 88px 放「标签 …… 数值」会比较挤，
中文标签超过 4 字就得截断。

新增常量：A2 需 `ATTR_FIXED_Y/H(20/65)`、`ATTR_UPGRADE_VIEWPORT_Y/H(91/32)`；A3 需 `ATTR_COLUMN_X(104)` + `ATTR_COLUMN_W(86)`。

### 7.4 组合采用记录

> **2026-09-17**：先按 **W2 + I2 + A3** 实现；后因双列在 88px 左列里截断中文标签、且构成行全部
> 落在折线以下，**属性页改回 A1（单列可折叠分节）**。本节其余内容保留为决策记录。
>
> **A1 的实现要点**：一份统一的 `contentHeight` 作为唯一真相源，绘制 / 高度 / 命中 / tooltip
> 全部从同一组推进项走出；§1 属性总览常驻且全宽，§2 构成 / §3 升级默认折叠、点开即整段展开
> （不再嵌套每行单独的折叠）。列常量 `ATTR_COL_X=40 / ATTR_COL_W=182` 在 `MythicMinerInfoLayout`，
> 由 `MythicMinerInfoLayoutTest.attributesColumnStaysInsideTheViewport` 锁定。

三页共享同一条原则：**会变的、要盯的数据常驻；只读的、静态的列表滚动。**

- 工作页 **W2** 让 8 项数据 + 3 个状态 chip 全部落在一屏，且补上了 W1 缺的「总并行」。
- 信息页 **I2** 去掉摘要行换来 14px 额外视口，全部平铺、不做折叠 —— 也就省掉了折叠状态下
  「每个分节高度都要动态算」这份脆弱性。
- 属性页先实现为 **A3**（两列共用滚动条），后因双列的 88px 左列截断中文标签、构成行全部
  落在折线以下，**改回 A1**：单列全宽读数常驻，构成与升级做成两个可折叠分节。

实现时确定下来的三点：

1. 工作页与信息页不需要折叠机制；**属性页（A1）则重新引入折叠**，但只有「分节」这一层
   （§2 / §3，不再嵌套每行单独的折叠）。`upgradeRowExpanded` 继续承载折叠状态，
   分节 key 用 `-100 / -101`（与历史 key 空间不冲突）。
2. 折叠版「统一 `contentHeight`」的教训被严格执行：**绘制 / 高度 / 命中 / tooltip 全部从同一组
   推进项走出**（§7.2 对 I1 的那份警告）。`rowKeyAt` 只识别两个分节头，其余行一律不响应点击。
3. 命中测试由页面自己回答（`InfoPage.productRowAt` / `AttributesPage.rowKeyAt`），
   分发层不复制布局知识，否则一旦某行展开就会点错行。


