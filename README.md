# SymAuto

> Sympsel 编写的适用于 Fabric 端 Minecraft **客户端**自动化 / 便利模组
> 按 `Shift + X` 打开公共功能菜单，全部功能均可在菜单中开关

- **运行环境**：Minecraft `26.2` + Fabric Loader + Fabric API (>=0.19.5) + 模组菜单
- **模块**：
  - `symbase`：公共前置，提供菜单、组件、功能注册、配置框架和 HUD 框架
  - `symauto`：自动化功能模块
  - `symhud`：HUD 元素、快捷容器面板、容器预览与高亮模块
- **开源协议**：[MIT](LICENSE)

## 特性
按分类提供 20+ 个可独立开关的功能（以游戏内菜单显示的中文名为准）：

### 自动化挂机
- 自动攻击、自动钓鱼、自动进食、自动奔跑
- 自动记录死亡信息（维度 / 坐标 / 死因，支持聊天列表与图形界面查看）

### 采集与容器
- 自动搜刮容器
- 快速将同类物品存入容器 / 取回背包
- 自动填充物品分类机

### 一键操作
- 一键丢弃垃圾物品（`Alt + Q`，支持白 / 黑名单）
- 一键丢弃同类物品、一键平铺物品
- 一键交易（快捷村民交易）

### 建造与破坏
- 快速工具（自动切换合适工具）
- 固定 Y 轴放置 / 破坏
- 快速搭桥

### 高亮 HUD
- 敌对生物高亮（支持黑 / 白名单、仅持武器时高亮等配置）
- 掉落物高亮、玩家高亮
- 特定村民高亮（依据本地交易缓存，附魔书黑 / 白名单）

### HUD 编辑
- 通用 HUD 编辑界面，所有已注册的 HUD 元素均可直接拖拽摆放
- 支持锚点自适应（左 / 中 / 右、上 / 中 / 下）、边缘与元素间吸附、8px 网格、方向键微调、缩放和显隐
- 布局保存到 `config/symbase/hud/layout.json`，窗口尺寸变化时贴边元素会保持相对位置
- 可通过功能菜单「打开 → HUD 编辑界面」或指令 `/sym:hud` 打开

#### 添加新的 HUD 元素

1. 继承 `AbstractHudElement`（纯文本可直接继承 `AbstractTextHudElement`），实现 `width`、`height`、`render`；
2. 在功能 MOD 的 `ClientModInitializer` 中调用 `HudManager.register(new YourHudElement())`。

注册后会自动获得布局持久化、HUD 渲染、编辑器中拖拽/吸附/缩放/显隐等能力，无需改编辑器代码。

### 便利与生产
- 便捷容器面板（支持收纳袋、潜影盒，潜影盒只能预览）（该部分实现参考 [BetterBundleHUDMod](https://github.com/aningQwQ/BetterBundleHUDMod-26.2/releases/tag/v1.3.0)）
- 多预设自动合成

### Mod Menu

- `SymAuto` 的 Mod Menu 配置入口打开公共功能菜单
- `SymHud` 的 Mod Menu 配置入口直接打开 HUD 编辑界面
- `SymBase` 标记为前置库，默认不出现在 Mod Menu 列表中；开启“显示库”后才会显示为 Library
### 注意
- 本项目目前不支持自定义组合快捷键绑定，推荐添加 mod [NewVisualKeybinding](https://www.mcmod.cn/class/27226.html) 或其它快捷键管理工具

## 命令

指令由各功能 MOD 通过 `symbase` 的 `CommandRegistry` 自行注册，统一使用 `sym:` 命名空间。

### 公共命令

| 命令 | 说明 |
|------|------|
| `/sym` 或 `/sym:menu` | 打开公共功能菜单 |
| `/sym:help` | 显示公共用法 |
| `/sym:list` | 列出全部功能（自动 + HUD） |
| `/sym:info <function>` | 查看任意功能详情 |
| `/sym:enable <function> [true\|false]` | 查询或设置任意功能开关 |
| `/sym:toggle <function>` | 切换任意功能开关 |

### SymAuto 专属命令

| 命令 | 说明 |
|------|------|
| `/sym:auto` 或 `/sym:auto:menu` | 打开功能菜单 |
| `/sym:auto:deaths [gui\|latest\|clear]` | 死亡记录：列表 / 界面 / 最近 / 清空 |
| `/sym:auto:bwlist <function> <blacklist\|whitelist> <list\|clear\|add\|remove> [id]` | 管理自动功能的黑白名单 |
| `/sym:auto:hud` | 打开 HUD 编辑界面 |

### SymHud 专属命令

| 命令 | 说明 |
|------|------|
| `/sym:hud` | 打开 HUD 编辑界面 |
| `/sym:hud:list` | 列出全部 HUD 功能 |
| `/sym:hud:info <function>` | 查看 HUD 功能详情 |
| `/sym:hud:enable <function> [true\|false]` | 查询或设置 HUD 功能开关 |
| `/sym:hud:toggle <function>` | 切换 HUD 功能开关 |
| `/sym:hud:trades count\|clear` | 查看 / 清空村民交易缓存 |
| `/sym:hud:bwlist <function> <blacklist\|whitelist> <list\|clear\|add\|remove> [id]` | 管理高亮功能的黑白名单 |

兼容别名：`/sym:a:*` 等价于 `/sym:auto:*`，`/sym:h:*` 等价于 `/sym:hud:*`。

## 配置

- 公共框架目录：`<游戏目录>/config/symbase/`
  - `functions.json`：**所有功能 MOD** 共用的功能开关、标签和功能选项
  - `hud/layout.json`：所有 HUD 元素的位置、缩放与显隐状态
- 自动化功能目录：`<游戏目录>/config/symauto/`
  - `bwlists/`：自动功能自己的黑 / 白名单
- HUD 功能目录：`<游戏目录>/config/symhud/`
  - `bwlists/`：高亮功能自己的黑 / 白名单
- 功能菜单底部提供「打开主配置文件夹」按钮，可直接在系统文件管理器中打开。

## 更新日志

更新日志按模块分开维护：

- `symauto/CHANGELOG.md` → `assets/symauto/update_log.md` → SymAuto 菜单「关于 → SymAuto 更新日志」
- `symhud/CHANGELOG.md` → `assets/symhud/update_log.md` → SymHud 菜单「关于 → SymHud 更新日志」

直接按 Markdown 修改：

```markdown
## 26.2-3.4.4 — 2026-11-01

### 新功能
- 新功能说明

### 修复
- Bug 修复说明
```

构建时各模块的 `CHANGELOG.md` 会自动复制到自己的资源目录，游戏内更新日志界面只读取对应模块的内容。不需要再写 `§` 颜色代码，也不需要修改 Java 源码。

## 构建

要求 **JDK 25**，在项目根目录执行：

```bash
# Windows
gradlew.bat buildAll
gradlew.bat release

# Linux / macOS
./gradlew buildAll
./gradlew release
```

产物：

- `symbase/build/libs/SymBase-*.jar`
- `symhud/build/libs/SymHud-*.jar`
- `symauto/build/libs/SymAuto-*.jar`
- `build/release/` 下是收集好的三个 MOD

`symauto` 和 `symhud` 都依赖 `symbase`；发布时需要同时安装 `symbase`。

开发环境统一使用 `:dev:runClient`，它使用根目录 `run/`，并通过 `-Pload` 选择加载哪些 MOD：

```bash
# 三个都加载
gradlew.bat :dev:runClient

# 只加载公共前置
gradlew.bat :dev:runClient -Pload=symbase

# 加载公共前置 + HUD 功能
gradlew.bat :dev:runClient -Pload=symhud

# 加载公共前置 + 自动化功能
gradlew.bat :dev:runClient -Pload=symauto
```

`:dev` 只用于开发启动，不参与 `buildAll` 和发布产物。

## 许可
本项目基于 MIT License 授权



