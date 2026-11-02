# SymAuto

Sympsel 编写的 Fabric Minecraft **客户端**模组系列。

按 `Shift + X` 打开公共功能菜单；所有功能都可以在菜单中独立开关

- **Minecraft**：`26.2`
- **Fabric Loader**：`>= 0.19.5`
- **Fabric API**：`0.161.0+26.2`
- **Java**：`25`
- **许可证**：[MIT](LICENSE)

## 模块

项目拆分为三个可独立发布的模块：

| 模块      | 作用                                                     | 必需 |
|-----------|----------------------------------------------------------|------|
| `symbase` | 公共前置：功能注册、配置、命令、菜单、HUD 框架、通用控件 | 是   |
| `symhud`  | HUD 元素、HUD 编辑器、容器面板与预览、实体/方块高亮      | 否   |
| `symauto` | 自动化、采集、建造、一键操作等自动功能                   | 否   |

依赖：

```text
symhud ─┐
        ├──> symbase
symauto ┘
```

`symhud` 与 `symauto` 互不依赖，可以单独安装；两者都必须安装 `symbase`

## 功能

### SymAuto：自动化

- 自动攻击、自动钓鱼、自动进食、自动奔跑
- 自动记录死亡信息：维度、坐标、死因，支持聊天列表与图形界面查看
- 自动搜刮容器
- 快速存取同类物品
- 自动填充物品分类机
- 一键丢弃垃圾/同类物品、一键平铺物品
- 快捷村民交易
- 快速工具自动切换
- 固定 Y 轴放置/破坏
- 快速搭桥
- 多预设自动合成

### SymHud：HUD 与高亮

- 通用 HUD 编辑界面：拖拽、锚点自适应、边缘/元素吸附、8px 网格、方向键微调、缩放、显示与隐藏
- 文字 HUD 元素颜色调节：选中文字元素后使用 R/G/B 滑杆调整，颜色随布局保存
- 全局 HUD 显隐：功能菜单中的「HUD 显示」，不会改变各元素自己的显隐状态
- 内置 HUD 元素：
  - 帧率
  - 主世界/当前维度坐标
  - 另一维度相对坐标
  - 系统时钟
  - 准心目标 ID（方块/实体/掉落物）
  - 准心目标距离（最远 64 格）
  - 盔甲与主副手装备耐久
  - 手持物品总数
- 快捷容器面板：支持收纳袋、潜影盒（潜影盒仅预览）
- 中键容器预览
- Shift + 悬浮容器预览
- 敌对生物、掉落物、玩家、特定村民高亮
- 支持扫描半径、黑白名单、附魔书过滤和村民交易缓存

## 命令

所有命令使用 `sym:` 命名空间

### 公共命令

| 命令                                   | 说明               |
|----------------------------------------|--------------------|
| `/sym` 或 `/sym:menu`                  | 打开公共功能菜单   |
| `/sym:help`                            | 显示公共用法       |
| `/sym:list`                            | 列出全部功能       |
| `/sym:info <function>`                 | 查看功能详情       |
| `/sym:enable <function> [true\|false]` | 查询或设置功能开关 |
| `/sym:toggle <function>`               | 切换功能开关       |

### SymAuto 命令

| 命令                                                                                 | 说明                                |
|--------------------------------------------------------------------------------------|-------------------------------------|
| `/sym:auto` 或 `/sym:auto:menu`                                                      | 打开功能菜单                        |
| `/sym:auto:help`                                                                     | 显示命令用法                        |
| `/sym:auto:list`                                                                     | 列出自动化功能                      |
| `/sym:auto:info <function>`                                                          | 查看功能详情                        |
| `/sym:auto:enable <function> [true\|false]`                                          | 查询或设置功能开关                  |
| `/sym:auto:toggle <function>`                                                        | 切换功能开关                        |
| `/sym:auto:deaths [gui\|latest\|clear]`                                              | 死亡记录：列表 / 界面 / 最近 / 清空 |
| `/sym:auto:bwlist <function> <blacklist\|whitelist> <list\|clear\|add\|remove> [id]` | 管理自动功能黑白名单                |
| `/sym:auto:hud`                                                                      | 打开 HUD 编辑界面                   |
| `/sym:auto:pilot`                                                                    | 打开试点界面                        |

其中 `/sym:a:*` 等价于 `/sym:auto:*`

### SymHud 命令

| 命令                                                                                | 说明                    |
|-------------------------------------------------------------------------------------|-------------------------|
| `/sym:hud`                                                                          | 打开 HUD 编辑界面       |
| `/sym:hud:list`                                                                     | 列出 HUD 功能           |
| `/sym:hud:info <function>`                                                          | 查看 HUD 功能详情       |
| `/sym:hud:enable <function> [true\|false]`                                          | 查询或设置 HUD 功能开关 |
| `/sym:hud:toggle <function>`                                                        | 切换 HUD 功能开关       |
| `/sym:hud:trades count\|clear`                                                      | 查看 / 清空村民交易缓存 |
| `/sym:hud:bwlist <function> <blacklist\|whitelist> <list\|clear\|add\|remove> [id]` | 管理高亮功能黑白名单    |

其中 `/sym:h:*` 等价于 `/sym:hud:*`

## 配置

配置按模块拆分，统一位于游戏目录的 `config/` 下：

```text
config/
├── symbase/
│   ├── functions.json        # 所有模块共用的功能开关、标签、功能选项
│   └── hud/
│       └── layout.json       # HUD 元素锚点、偏移、缩放、显隐、文字颜色
├── symauto/
│   └── bwlists/              # 自动功能黑白名单
└── symhud/
    └── bwlists/              # 高亮功能黑白名单
```

- 功能菜单位置、开关、标签和通用选项保存在 `config/symbase/functions.json`
- HUD 布局保存在 `config/symbase/hud/layout.json`，删除该文件会恢复默认布局
- 功能菜单提供「打开主配置文件夹」入口

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

构建产物：

```text
symbase/build/libs/SymBase-*.jar
symhud/build/libs/SymHud-*.jar
symauto/build/libs/SymAuto-*.jar
build/release/ # 收集好的三个 MOD 汇总
```

### 开发启动

`:dev:runClient`，它使用根目录 `run/`，通过 `-Pload` 选择加载的模块：

```bash
# 三个都加载
gradlew.bat :dev:runClient

# 只加载公共前置
gradlew.bat :dev:runClient "-Pload=symbase"

# 公共前置 + HUD
gradlew.bat :dev:runClient "-Pload=symhud"

# 公共前置 + 自动化
gradlew.bat :dev:runClient "-Pload=symauto"

# HUD + 自动化
gradlew.bat :dev:runClient "-Pload=symhud,symauto"
```

更多启动说明见 [RUN.md](RUN.md)

## 自主开发

### 添加功能

1. 继承 `SymAbstractFunction`
2. 实现 `onTrigger`；
3. 在模块的 `ClientModInitializer` 中注册 `FunctionRegistry.register(function)`
4. 如果需要绑定快捷键，重写 `getKeyMapping()`

### 添加 HUD 元素

1. 继承 `AbstractHudElement`，或纯文本元素继承 `AbstractTextHudElement`
2. 实现 `width`、`height`、`render`
3. 在模块初始化时调用 `HudManager.register(new YourHudElement())`

文字元素继承 `AbstractTextHudElement` 后自动获得文字颜色属性、HUD 编辑器中的 R/G/B 滑杆和布局持久化能力

### 模块约束

- `symbase` 不能反向依赖 `symhud` 或 `symauto`
- `symhud` 与 `symauto` 不能互相依赖
- 各模块自己的功能、存储和界面放在各自模块

## 更新日志

更新日志按模块维护：

构建时 Gradle 会自动复制`CHANGELOG.md`到对应资源目录

- `symauto/CHANGELOG.md` → `assets/symauto/update_log.md`
- `symhud/CHANGELOG.md` → `assets/symhud/update_log.md`

## 许可

本项目基于 [MIT License](LICENSE) 授权
