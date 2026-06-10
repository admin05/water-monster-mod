# water-monster-mod

A Fabric mod for Minecraft 1.21.11 focused on an upgraded Water Monster enemy and the TNT Fishing Rod.

## Features

- Adds a Water Monster entity that is summoned from a survival-friendly sandstone altar.
- The Water Monster has three 100-health phases, for 300 total health.
- The Water Monster displays a boss health bar that updates with its current phase.
- In phase 1, the Water Monster mirrors the nearby player's equipment, stance, item use, and attack rhythm.
- In phase 2, the Water Monster actively attacks survival players and flexibly uses copied weapons, tools, shields, Totems of Undying, movement items, and healing supplies.
- In phase 3, the Water Monster adds a TNT rail cannon attack using the existing custom non-griefing TNT.
- On flat ground, the Water Monster prefers sword and axe combat.
- The Water Monster can use the TNT Fishing Rod to summon falling TNT in multiple concentric rings.
- Right-clicking the TNT Fishing Rod summons layered TNT rings from above.
- Left-clicking a block with the TNT Fishing Rod toggles whether summoned TNT can break blocks.
- The Water Monster drops the TNT Fishing Rod when killed.
- Custom TNT protects terrain by default and only damages blocks when block-breaking mode is enabled.
- The Water Monster no longer spawns naturally in water.
- Build a 3-sandstone base line with 1 sandstone block centered on top, then right-click the top sandstone with an empty main hand to summon the Water Monster. The altar remains in place after summoning.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.0 or newer
- Fabric API
- Java 21

## Build

```bash
./gradlew build
```

If your default Java version is not 21, specify Java 21 explicitly:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/liberica-jdk-21.jdk/Contents/Home ./gradlew build
```

The built mod jar will be generated at:

```text
build/libs/模组-1.0.0.jar
```

## Installation

1. Install Minecraft Fabric Loader.
2. Install Fabric API.
3. Put `build/libs/模组-1.0.0.jar` into the game `mods` folder.
4. Launch the game.

## Main Files

- `src/main/java/com/example/examplemod/ExampleMod.java`: Registers items, entities, altar summoning, and events.
- `src/main/java/com/example/examplemod/entity/WaterMonsterEntity.java`: Water Monster AI and combat logic.
- `src/main/java/com/example/examplemod/entity/NoBlockDamageTntEntity.java`: Custom TNT entity with optional block damage.
- `src/main/java/com/example/examplemod/item/TntFishingRodItem.java`: TNT Fishing Rod usage and mode switching.
- `src/client/java/com/example/examplemod/ExampleModClient.java`: Client-side renderer registration.

## Development

Common commands:

```bash
./gradlew build
./gradlew runClient
```

This project uses the Gradle Wrapper, so a separate Gradle installation is not required.

## License

MIT

---

# water-monster-mod

这是一个基于 Fabric 的 Minecraft 1.21.11 模组，核心内容是强化版水怪和 TNT Fishing Rod。

## 功能

- 新增水怪实体，可通过生存模式可完成的沙岩祭坛召唤。
- 水怪拥有三个各 100 点血的阶段，总生命值 300。
- 水怪会显示 Boss 血条，并随当前阶段更新显示。
- 1 阶段会复刻附近玩家的装备、姿态、物品使用和攻击节奏。
- 2 阶段会主动攻击生存玩家，并灵活使用复制到的武器、工具、盾牌、不死图腾、位移道具和治疗物资。
- 3 阶段会追加使用基于现有自定义 TNT 的 TNT 轨道炮攻击。
- 水怪在平地会优先使用剑和斧近战。
- 水怪会使用 TNT Fishing Rod 召唤多层同心圆环 TNT。
- TNT Fishing Rod 右键会从上空召唤下落的多层圆环 TNT。
- TNT Fishing Rod 左键方块可切换召唤的 TNT 是否破坏方块。
- 水怪被击杀后会掉落 TNT Fishing Rod。
- 自定义 TNT 默认不会破坏地形，只有开启破坏模式后才会破坏方块。
- 水怪不再在水中自然生成。
- 用 3 个沙岩摆成底部一横排，再在中间沙岩上方放 1 个沙岩；主手空手右键顶部沙岩即可召唤水怪，召唤后祭坛不会消失。

## 环境要求

- Minecraft 1.21.11
- Fabric Loader 0.19.0 或更高版本
- Fabric API
- Java 21

## 构建

```bash
./gradlew build
```

如果系统默认 Java 版本不是 21，可以指定 Java 21：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/liberica-jdk-21.jdk/Contents/Home ./gradlew build
```

构建完成后的模组文件位于：

```text
build/libs/模组-1.0.0.jar
```

## 安装

1. 安装 Minecraft Fabric Loader。
2. 安装 Fabric API。
3. 将 `build/libs/模组-1.0.0.jar` 放入游戏目录的 `mods` 文件夹。
4. 启动游戏。

## 主要文件

- `src/main/java/com/example/examplemod/ExampleMod.java`：物品、实体、祭坛召唤和事件注册。
- `src/main/java/com/example/examplemod/entity/WaterMonsterEntity.java`：水怪 AI 和战斗逻辑。
- `src/main/java/com/example/examplemod/entity/NoBlockDamageTntEntity.java`：可选破坏方块的自定义 TNT 实体。
- `src/main/java/com/example/examplemod/item/TntFishingRodItem.java`：TNT Fishing Rod 使用和模式切换逻辑。
- `src/client/java/com/example/examplemod/ExampleModClient.java`：客户端渲染注册。

## 开发

常用命令：

```bash
./gradlew build
./gradlew runClient
```

项目使用 Gradle Wrapper，不需要单独安装 Gradle。

## 许可证

MIT
