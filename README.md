# Data Recipe Generator

在游戏内用**原生容器界面**摆放材料，一键生成可加载的数据包配方 JSON。  
支持 **Forge 1.20.1**、**NeoForge 1.21.1** 与 **NeoForge 26.1.2（Minecraft 26.1.2）**。

---

## 1. 功能概览

- 指令打开界面：`/datarecipe` 或 `/drg`，也可以直接 `/drg furnace`、`/drg smithing_trim` 等。
- 顶部标签切换容器；左侧是原生容器纹理和真实槽位，右侧是配方选项面板。
- 服务端读取菜单槽位生成配方，客户端只发送选项，不发送物品数据。
- 保留四种文件名模式：普通、自动、半动态、动态继承（输入→输出）。
- 生成结果直接写成**可用的数据包目录**，包含 `pack.mcmeta` 和 `data/<namespace>/recipe(s)/`。
- 路径写入限制在 `data_recipe_generator_output` 内，拒绝 `..`、绝对路径等越权写法。

## 2. 支持的容器与配方类型

| 容器 | Forge 1.20.1 | NeoForge 1.21.1 | NeoForge 26.1.2 |
|---|---|---|---|
| 工作台 | `crafting_shaped`、`crafting_shapeless` | `crafting_shaped`、`crafting_shapeless` | 同上，另有 `crafting_transmute`、`crafting_imbue` |
| 熔炉 | `smelting` | `smelting` | `smelting` |
| 高炉 | `blasting` | `blasting` | `blasting` |
| 烟熏炉 | `smoking` | `smoking` | `smoking` |
| 营火 | `campfire_cooking` | `campfire_cooking` | `campfire_cooking` |
| 切石机 | `stonecutting` | `stonecutting` | `stonecutting` |
| 锻造台 | `smithing_transform`、`smithing_trim` | `smithing_transform`、`smithing_trim` | `smithing_transform`、`smithing_trim` |

**不在列表中的容器**（酿造台、铁砧、砂轮、附魔台、织布机、制图台、信标等）在原版里没有数据包配方 JSON，因此无法生成；这不是遗漏。

版本差异：

- **1.20.1**：`recipes` 目录；成分写成 `{"item": ...}` / `{"tag": ...}` 对象；工作台/锻造结果写成 `{"item": "...", "count": n}`；**熔炼类结果必须是字符串、数量固定为 1**；切石机写成 `result` 字符串 + `count`；锻造纹饰的图案由模板物品决定，JSON 里没有 `pattern` 字段。
- **1.21.1**：`recipe` 目录；成分仍是 `{"item": ...}` / `{"tag": ...}` 对象；结果写成 `{"id": "...", "count": n}`；熔炼结果是 `{"id": "..."}`（数量固定 1，无 `count` 字段）；切石机保留 `group`；锻造升级三件套必填；锻造纹饰仍由模板物品决定，没有 `pattern` 字段。
- **26.1.2**：`recipe` 目录；成分写成字符串 holder set（`"minecraft:stone"` / `"#tag"`）；结果写成 `{"id": "...", "count": n}`；切石机结果也是对象且不再有 `group`；熔炼类保留数量；锻造纹饰需要 `pattern` 字段（默认 `minecraft:sentry`）。

## 3. 使用流程

1. 输入 `/drg`（默认打开工作台）或 `/drg <容器>`。
2. 用顶部标签切换容器，用右侧第一行按钮切换配方类型。
3. 像操作原生界面一样，把材料放进对应槽位，把想要的**成品**放进输出槽。
4. 设置命名空间、命名模式、文件名、输出子文件夹等，点击 **Generate recipe / 生成配方**。
5. 成功后面板状态区和聊天栏会显示生成路径。

### 指令

| 指令 | 打开的容器 / 配方类型 |
|---|---|
| `/drg`、`/drg crafting` | 工作台（有序） |
| `/drg crafting_shaped` | 工作台（有序） |
| `/drg crafting_shapeless` | 工作台（无序） |
| `/drg transmute` | 工作台（转化，仅 26.1.2） |
| `/drg imbue` | 工作台（注魔，仅 26.1.2） |
| `/drg furnace`、`/drg smelting` | 熔炉 |
| `/drg blast_furnace`、`/drg blasting` | 高炉 |
| `/drg smoker`、`/drg smoking` | 烟熏炉 |
| `/drg campfire`、`/drg campfire_cooking` | 营火 |
| `/drg stonecutter`、`/drg stonecutting` | 切石机 |
| `/drg smithing`、`/drg smithing_transform` | 锻造台（升级） |
| `/drg smithing_trim` | 锻造台（纹饰） |

权限：OP 权限等级 2；单人存档的存档主即使没开作弊也可以使用。

### 槽位角色

- **有序 / 无序**：3×3 工作台槽位 + 输出槽。有序配方会自动裁掉空行/空列。
- **转化（26.1.2）**：**中心槽**是输入物品，其余非空槽是材料；所有材料必须是同一种物品。
- **注魔（26.1.2）**：**中心槽**是来源（例如药水），其余 8 个槽必须填满且为同一种材料。
- **熔炼 / 高炉 / 烟熏 / 营火**：输入槽 + 输出槽；燃料槽不参与配方，会被忽略。
- **切石**：输入槽 + 输出槽。
- **锻造升级**：模板、基础物品、附加材料、成品。
- **锻造纹饰**：模板、基础物品、附加材料；26.1.2 还需要填写纹饰图案 ID，1.20.1/1.21.1 由模板物品决定。

### 命名模式

| 模式 | 生成的默认文件名 |
|---|---|
| 普通 / custom | 用户填写的文件名；留空则用品 ID |
| 自动 / auto | 成品 ID（如 `diamond_sword`） |
| 半动态 / auto+suffix | 成品 ID + `_` + 用户后缀 |
| 动态继承 / in→out | 输入 ID + `_to_` + 成品 ID（如 `iron_ingot_to_iron_block`） |

锻造纹饰的自动名称为 `基础物品_trim[_图案]`。所有文件名会统一转为小写并把不安全字符替换为 `_`。

### 输出位置

默认写到服务端目录下：

```text
<服务器目录>/data_recipe_generator_output/
├── pack.mcmeta
└── data/<namespace>/
    ├── recipe/<file>.json      # 1.21.1 / 26.1.2
    └── recipes/<file>.json     # 1.20.1
```

“输出文件夹”字段可以填写 `data_recipe_generator_output` 下的相对子文件夹，例如 `my_pack`，用于把不同配方分到不同数据包。留空表示直接写到根目录。

把 `data_recipe_generator_output`（或其中的子文件夹）放进世界的 `datapacks/`，然后执行 `/reload` 即可加载。`pack.mcmeta` 已按对应版本写好。

## 4. 构建与运行

环境要求：

- Gradle Wrapper 9.1.0（仓库已包含）。
- Forge 1.20.1 使用 Java 17。
- NeoForge 1.21.1 使用 Java 21。
- NeoForge 26.1.2 使用 Java 25；如果本机没有 JDK 25，foojay 工具链解析器会自动下载。
- 首次构建需要联网下载 Forge/NeoForge 的 userdev、Minecraft、映射等依赖。

```bash
# 构建全部模块（含 common 单测）
./gradlew build

# 只构建三个平台 jar
./gradlew buildAll

# 客户端
./gradlew :forge-1.20.1:runClient
./gradlew :neoforge-1.21.1:runClient
./gradlew :neoforge-26.1.2:runClient

# 服务端
./gradlew :forge-1.20.1:runServer
./gradlew :neoforge-1.21.1:runServer
./gradlew :neoforge-26.1.2:runServer
```

产物：

```text
forge-1.20.1/build/libs/data_recipe_generator-forge-1.20.1-1.0.0.jar
neoforge-1.21.1/build/libs/data_recipe_generator-neoforge-1.21.1-1.0.0.jar
neoforge-26.1.2/build/libs/data_recipe_generator-neoforge-26.1.2-1.0.0.jar
```

只把对应平台的 jar 放进 `mods/`；`common` 是编译期共享模块，它的 class 已经折叠进三个平台 jar，不要单独安装。

## 5. 项目结构

```text
data_recipe_generator/
├── common/                 # 与 Minecraft 无关的共享层
│   ├── 配方模型、JSON 生成、命名模式、路径校验、数据包写入
│   └── 单元测试（不依赖 Minecraft）
├── forge-1.20.1/           # Forge 1.20.1 平台
│   ├── 指令、菜单、SimpleChannel 网络、GuiGraphics 原生界面
│   └── META-INF/mods.toml
├── neoforge-1.21.1/        # NeoForge 1.21.1 平台
│   ├── 指令、菜单、Payload 网络、GuiGraphics 原生界面
│   └── META-INF/neoforge.mods.toml
├── neoforge-26.1.2/        # NeoForge 26.1.2 平台
│   ├── 指令、菜单、Payload 网络、GuiGraphicsExtractor 原生界面
│   └── META-INF/neoforge.mods.toml
├── build.gradle            # 根构建：共享约定 + buildAll
└── settings.gradle         # 四个 Gradle 子模块（common + 三个平台）
```

设计要点：

- `common` 不引用任何 Minecraft 类，只处理字符串 ID、JSON、命名和文件路径。它的 class 会被折叠进三个平台 jar，不产生额外的运行时依赖。
- 三个平台只负责：把菜单槽位转成 `SlotStack`、注册菜单/网络/指令、绘制屏幕。
- 生成时服务端读取自己的菜单槽位，客户端不能伪造物品；关闭界面或切换标签时，临时槽位里的物品会退回玩家背包。
- 26.1.2 的 GUI 使用 1.21.9+ 的 `GuiGraphicsExtractor` 新渲染模型；1.20.1 与 1.21.1 仍使用 `GuiGraphics`。

## 6. 已知限制

- 1.20.1 与 1.21.1 的熔炼/高炉/烟熏/营火配方输出数量固定为 1（原版格式限制）；26.1.2 可以保留数量。
- 生成结果只记录**物品 ID + 数量**，不写入 NBT/数据组件。需要附魔、药水内容等特殊结果的配方，生成后需要手动补组件。
- 原版由代码实现的特殊配方（烟花、旗帜、地图复制/扩展、修复、染色、书复制等）没有数据包 JSON，无法生成。
- 当前 GUI 直接读取槽位物品；`ingredientOverrides`（`#tag` 成分）已在共享层实现，但尚未接入界面，属于后续扩展点。
- 1.20.1 与 1.21.1 的锻造纹饰不需要也不支持 `pattern` 字段；26.1.2 必须填写有效的 `trim_pattern` ID。
- 生成的数据包不会自动放进 `datapacks/`，需要手动复制并 `/reload`。

## 7. 验证状态

- `./gradlew build` / `./gradlew buildAll` 通过；`common` 的 21 个单元测试全部通过。
- **NeoForge 26.1.2 专用服务器**：模组正常加载；`/datarecipe`、`/drg` 及全部子指令注册成功；用本模组生成的 8 种配方（有序、无序、熔炼、切石、锻造升级、锻造纹饰、转化、注魔）放入数据包后，服务器配方数从 1515 增加到 1523，`/reload` 正常。
- **NeoForge 1.21.1 专用服务器**：模组正常加载；`/datarecipe`、`/drg` 指令树注册成功（1.21.1 没有转化/注魔子指令）；6 种生成配方放入数据包后，服务器配方数从 1290 增加到 1296，`/reload` 正常。
- **Forge 1.20.1 专用服务器**：用官方安装器搭建的干净服务端正常加载模组；`/datarecipe`、`/drg` 指令树注册成功。1.20.1 的 6 种配方 JSON 已用原版 1.20.1 服务端解析验证，无配方解析错误。
