# STS Controller Vibration Support

`STS Controller Vibration Support` 是一个给《杀戮尖塔》提供手柄振动反馈的 Mod。

目前支持的振动事件：

- 角色受到生命伤害
- 玩家对敌人造成生命伤害
- 烧牌
- 丢弃牌
- 角色选择
- 问号房 / Neow 选项导航
- 问号房 / Neow 选项确认
- 获得格挡
- 伤害被格挡
- 升级牌三段节奏振动
- 打开宝箱

后端顺序：

- 优先使用 Steam Input
- Windows 下回退到 XInput

## 特性

- 每个振动事件都有固定 `id`、显示名称和独立配置项
- 配置界面支持中英文 i18n
- 每个事件都可以单独调整 `0% - 100%` 强度
- 高频事件支持最小触发间隔，避免振动疲劳
- 支持延迟脉冲和多段节奏振动

## 项目结构

- `src/main/java/stscontrollervibration/ControllerVibrationMod.java`
  - Mod 入口，负责初始化和更新
- `src/main/java/stscontrollervibration/vibration/VibrationManager.java`
  - 高层振动注册、配置、触发、迁移
- `src/main/java/stscontrollervibration/rumble/RumbleManager.java`
  - 底层振动队列和后端分发
- `src/main/java/stscontrollervibration/patches`
  - 各个游戏事件的 patch 触发点
- `src/main/resources/localization`
  - 配置界面和事件名称本地化

## 构建

要求：

- JDK，且 `javac`、`jar` 在 `PATH` 中
- 本机已安装 Slay the Spire、ModTheSpire、BaseMod

仅构建：

```powershell
./build.ps1
```

构建并安装到自动识别的 `mods` 目录：

```powershell
./build.ps1 -Install
```

构建产物：

```text
build/StsControllerVibrationSupport.jar
```

## 开发

新增振动事件的完整说明见：

- [docs/ADDING_VIBRATIONS.md](docs/ADDING_VIBRATIONS.md)

开发时的几条约束：

- 不要直接在业务 patch 里调用底层后端，统一通过 `VibrationManager.trigger(...)`
- 每个新振动都要先注册，再暴露到配置和 i18n
- 如果你修改了已有事件的默认值，要同时考虑配置迁移，避免覆盖玩家已经手调过的配置
- `SpirePatch2` 的参数名要和目标方法真实参数名一致；如果不需要目标参数，宁可不写

## 已知说明

- `build.ps1` 会自动从常见 Steam 路径查找 `desktop-1.0.jar`、`ModTheSpire.jar` 和 `BaseMod.jar`
- Steam Input 优先是为了避免和其他 Windows 手柄后端重复振动
- 宝箱逻辑当前通过房间 `update()` 监听开启状态，而不是直接挂在 `AbstractChest.open(...)` 核心奖励链上
