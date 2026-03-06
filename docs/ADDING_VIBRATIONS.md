# 新增振动事件指南

这份文档约定一个原则：

所有业务层振动都必须先在 `VibrationManager` 注册，再通过 `VibrationManager.trigger(...)` 触发。

不要在 patch 里直接调用 `RumbleManager`。

## 一次新增通常要改哪些地方

最少会动这几类文件：

- `src/main/java/stscontrollervibration/vibration/VibrationManager.java`
- `src/main/java/stscontrollervibration/patches/...`
- `src/main/resources/localization/strings_en.properties`
- `src/main/resources/localization/strings_zh.properties`

如果你修改的是已有事件的默认值，还要额外处理配置迁移：

- `src/main/java/stscontrollervibration/vibration/VibrationManager.java`

## 步骤 1：定义事件 ID

先在 `VibrationManager` 顶部添加一个新的常量，例如：

```java
public static final String RELIC_OBTAINED_ID = "relic_obtained";
```

要求：

- `id` 必须稳定，后续不要随意改名
- `id` 会进入配置键名，也会进日志
- 新旧版本之间最好保持兼容

## 步骤 2：注册事件

在 `VibrationManager.ensureBuiltInsRegistered()` 里注册：

```java
register(
    RELIC_OBTAINED_ID,
    LocalizationManager.text("vibration.relic_obtained.name"),
    72,
    500L,
    magnitude -> RumbleSpec.pattern(
        RumbleSpec.pulse(0.46f, 0.88f, 65L, 0L),
        RumbleSpec.pulse(0.22f, 0.40f, 70L, 75L)
    )
);
```

参数说明：

- 第 1 个参数：稳定 `id`
- 第 2 个参数：显示名，必须走 i18n
- 第 3 个参数：默认强度百分比，范围 `0-100`
- 第 4 个参数：最小触发间隔，单位毫秒，用来限制高频事件刷震
- 第 5 个参数：`VibrationFactory`，根据 `magnitude` 生成真正的振动波形

## 步骤 3：选择合适的波形

常用 API：

- `RumbleSpec.of(left, right, durationMs)`
  - 单段振动
- `RumbleSpec.normalized(left, right, durationMs)`
  - 会自动按峰值归一化，适合“只想写相对比例”的短脉冲
- `RumbleSpec.pattern(...)`
  - 多段节奏振动
- `RumbleSpec.pulse(left, right, durationMs, delayMs)`
  - 一个脉冲片段

设计建议：

- 高频事件优先短、轻、明确
- 低频大事件可以用两段或三段节奏
- 如果同类事件在一回合里会连发，一定要给 `minIntervalMs`
- `magnitude` 最好传真实语义值，比如伤害值、格挡值、牌数，而不是固定 `1`

## 步骤 4：补 i18n

在中英文资源里都加一条：

`src/main/resources/localization/strings_en.properties`

```properties
vibration.relic_obtained.name=Relic Obtained
```

`src/main/resources/localization/strings_zh.properties`

```properties
vibration.relic_obtained.name=获得遗物
```

如果缺少翻译，运行时会回退到英文，但不要依赖这个兜底。

## 步骤 5：写 patch 触发点

新增一个 patch 类，在正确的游戏时机触发：

```java
@SpirePatch2(clz = AbstractRelic.class, method = "instantObtain")
public class RelicObtainedPatch {
    public static void Postfix(AbstractRelic __instance) {
        VibrationManager.trigger(VibrationManager.RELIC_OBTAINED_ID);
    }
}
```

建议：

- 业务 patch 只负责“判定是否触发”和“传入 magnitude”
- 振动强度、节奏、限流都放在 `VibrationManager`
- 如果事件有重复触发风险，先确认是否应该在 patch 里过滤一次，再依赖 `minIntervalMs`

## 步骤 6：如果改的是已有默认值，处理配置迁移

不要直接改默认值就结束，否则老玩家已经保存过的配置不会自动更新。

当前做法是：

- 提升 `CURRENT_CONFIG_VERSION`
- 增加一个 migration 方法
- 只迁移“仍然等于旧默认值”的配置

这样可以做到：

- 没手调过配置的玩家自动吃到新默认
- 手动调过的玩家保留自己的选择

## 步骤 7：构建和验证

构建：

```powershell
./build.ps1
```

安装：

```powershell
./build.ps1 -Install
```

验证时重点看：

- 游戏是否正常启动
- `ModTheSpire` patch 是否成功注入
- 日志里有没有 `sts_controller_vibration ready`
- 日志里有没有 `Rumble environment:`
- 新事件是否出现在配置界面
- 配置页中英文是否正常显示

## 常见坑

### 1. `SpirePatch2` 参数名不匹配

`ModTheSpire` 会按参数名绑定。

如果你写了目标方法的参数名，就必须和反编译出来的真实参数名一致；如果不需要这个参数，直接删掉，不要乱写。

### 2. 在错误的核心函数上挂 patch

像 `AbstractChest.open(...)` 这种原版核心奖励链，兼容性风险高。

优先选择：

- 更外层但语义仍然稳定的时机
- 房间或界面的状态切换点
- 已经完成原版副作用之后的安全时机

### 3. 直接调用底层后端

不要在 patch 里直接 `RumbleManager.queue(...)`。

否则你会绕过：

- 配置强度
- i18n 命名
- 统一注册
- 事件级限流
- 默认值和迁移逻辑

### 4. 高频事件不做限流

弃牌、烧牌、格挡、导航这些都很容易疲劳。

如果你不确定，先给一个保守的 `minIntervalMs`，再通过实机手感慢慢调。

## 推荐开发流程

1. 先找到最稳的游戏触发点
2. 先注册事件并补双语名字
3. 先用保守波形和保守默认值
4. 通过 `magnitude` 把真实语义值接进来
5. 加最小触发间隔
6. 实机测试手感，再调强度和节奏

## 当前架构速记

- `ControllerVibrationMod`
  - Mod 生命周期入口
- `VibrationManager`
  - 事件注册、配置、迁移、触发
- `RumbleManager`
  - 底层调度、排队、分发
- `XInputManager`
  - Windows XInput 后端
- `LocalizationManager`
  - 配置界面和事件名称的中英文加载

新增事件时，优先把语义留在 `VibrationManager`，把 patch 保持薄。
