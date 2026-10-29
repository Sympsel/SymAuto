# SymAuto

> Sympsel 编写的适用于 Fabric 端 Minecraft **客户端**自动化 / 便利模组
> 按 `Shift + X` 打开功能菜单，全部功能均可在菜单中开关

- **运行环境**：Minecraft `26.2` + Fabric Loader + Fabric API (>=0.19.5) + 模组菜单
- **模组 ID**：`symauto`
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

### 便利与生产
- 便捷容器面板（支持收纳袋、潜影盒，潜影盒只能预览）（该部分实现参考 [BetterBundleHUDMod](https://github.com/aningQwQ/BetterBundleHUDMod-26.2/releases/tag/v1.3.0)）
- 多预设自动合成

### 注意
- 本项目目前不支持自定义组合快捷键绑定，推荐添加 mod [NewVisualKeybinding](https://www.mcmod.cn/class/27226.html) 或其它快捷键管理工具

## 命令

提供命名空间式客户端命令，根命令 `symauto` 与简写 `sa` 等价（例：`/sa:list`、`/symauto:list`）：

| 命令                                                                           | 说明                                |
|--------------------------------------------------------------------------------|-------------------------------------|
| `/sa` 或 `/sa:menu`                                                            | 打开功能菜单                        |
| `/sa:help`                                                                     | 显示用法                            |
| `/sa:list`                                                                     | 列出全部功能及开关状态              |
| `/sa:info <function>`                                                          | 查看功能详情                        |
| `/sa:enable <function> [true\|false]`                                          | 查询或设置功能开关                  |
| `/sa:toggle <function>`                                                        | 切换功能开关                        |
| `/sa:trades count\|clear`                                                      | 查看 / 清空村民交易缓存             |
| `/sa:deaths [gui\|latest\|clear]`                                              | 死亡记录：列表 / 界面 / 最近 / 清空 |
| `/sa:bwlist <function> <blacklist\|whitelist> <list\|clear\|add\|remove> [id]` | 管理各功能的黑白名单                |

## 配置

- 配置文件目录：`<游戏目录>/config/symauto/`
  - `symauto.json`：功能开关与各功能选项
  - `bwlists/`：各功能的黑 / 白名单（每个功能的每种名单为独立 JSON 文件）
- 功能菜单底部提供「打开配置文件夹」按钮，可直接在系统文件管理器中打开。

## 构建

要求 **JDK 25**，在项目根目录执行：

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

## 许可
本项目基于 MIT License 授权