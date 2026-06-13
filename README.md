# water-monster-mod

A Fabric mod for Minecraft 1.21.11 focused on an upgraded Water Monster enemy and the TNT Fishing Rod.

## Features

- Adds a Water Monster entity that is summoned from a survival-friendly crying obsidian altar.
- The Water Monster has three 100-health phases, for 300 total health.
- The Water Monster displays a boss health bar that updates with its current phase.
- In phase 1, the Water Monster only mirrors the nearby player's equipment, stance, item use, and attack rhythm like a player reflection.
- In phase 2, the Water Monster starts actively attacking survival players and flexibly uses copied weapons, tools, shields, Totems of Undying, movement items, and healing supplies.
- In phases 1 and 2, each Water Monster randomly chooses one of the bundled Steve-style skins and keeps that skin until phase 3.
- In phase 3, the Water Monster switches to the bundled phase-three Steve-style skin.
- In phase 3, the Water Monster adds a TNT rail cannon attack using the existing custom non-griefing TNT.
- On flat ground, the Water Monster prefers sword and axe combat.
- The Water Monster can use the TNT Fishing Rod to summon falling TNT in multiple concentric rings.
- Right-clicking the TNT Fishing Rod summons layered TNT rings from above.
- Left-clicking a block with the TNT Fishing Rod toggles whether summoned TNT can break blocks.
- The Water Monster drops the TNT Fishing Rod when killed.
- Custom TNT protects terrain by default and only damages blocks when block-breaking mode is enabled.
- The Water Monster no longer spawns naturally in water.
- Build a 2-layer crying obsidian altar: the first layer is a 5-block cross, and the second layer is 1 crying obsidian block centered above it. Right-click the upper block with an empty main hand to summon the Water Monster. The altar remains in place after summoning.
- Summoning now plays a staged entrance ritual with a pale-blue sky circle and Chinese-inspired sky particle patterns including bagua, huiwen borders, talisman strokes, and seal marks, beams striking the altar, random sky bolts, crying obsidian tear particles, Enderman portal particles, soul particles, soul fire, orange-red flame, and a non-griefing explosion burst.
- From phase 2 onward, summoned Water Monsters defend their 6 altar crying obsidian blocks from players.
- TNT, creepers, and other explosions cannot destroy the bound altar crying obsidian blocks.
- Only players can break the bound altar crying obsidian blocks. Breaking each of the first 5 blocks permanently lowers the Water Monster's maximum health by 45. Breaking the 6th block does not deal damage, but stops the Water Monster from actively healing.
- After all 6 altar blocks are broken, the Water Monster only has a 20% chance to copy a player's healing action.

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

- 新增水怪实体，可通过生存模式可完成的哭泣黑曜石祭坛召唤。
- 水怪拥有三个各 100 点血的阶段，总生命值 300。
- 水怪会显示 Boss 血条，并随当前阶段更新显示。
- 1 阶段只会像玩家镜像一样复刻附近玩家的装备、姿态、物品使用和攻击节奏。
- 2 阶段才会开始主动攻击生存玩家，并灵活使用复制到的武器、工具、盾牌、不死图腾、位移道具和治疗物资。
- 1 到 2 阶段水怪会从内置的 3 张 Steve 风格皮肤里随机选择 1 张，并保持到进入 3 阶段。
- 3 阶段水怪会切换为内置的 3 阶段 Steve 风格皮肤。
- 3 阶段会追加使用基于现有自定义 TNT 的 TNT 轨道炮攻击。
- 水怪在平地会优先使用剑和斧近战。
- 水怪会使用 TNT Fishing Rod 召唤多层同心圆环 TNT。
- TNT Fishing Rod 右键会从上空召唤下落的多层圆环 TNT。
- TNT Fishing Rod 左键方块可切换召唤的 TNT 是否破坏方块。
- 水怪被击杀后会掉落 TNT Fishing Rod。
- 自定义 TNT 默认不会破坏地形，只有开启破坏模式后才会破坏方块。
- 水怪不再在水中自然生成。
- 用哭泣黑曜石搭 2 层祭坛：第 1 层是 5 个方块的十字形，第 2 层是在中心上方放 1 个哭泣黑曜石；主手空手右键上方方块即可召唤水怪，召唤后祭坛不会消失。
- 召唤时会播放分阶段登场仪式，包含空中淡蓝色圆阵、八卦/回纹边框/符箓笔画/印章感纹样、直射祭坛的光束、随机下射光束、哭泣黑曜石紫泪粒子、末影人传送紫色粒子、灵魂粒子、淡蓝色魂火、橙红色火焰和不破坏方块的爆炸登场效果。
- 召唤出的水怪从 2 阶段开始会守护这 6 个祭坛哭泣黑曜石，玩家尝试破坏时水怪会主动攻击玩家。
- TNT、苦力怕等爆炸无法破坏绑定的祭坛哭泣黑曜石。
- 只有玩家可以破坏绑定的祭坛哭泣黑曜石。破坏前 5 个时，每破坏 1 个，水怪永久损失 45 点最大生命，无法恢复；破坏第 6 个不会造成伤害，但水怪不再主动回血。
- 6 个祭坛哭泣黑曜石都被破坏后，水怪在玩家回血时只有 20% 概率复刻回血。

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
