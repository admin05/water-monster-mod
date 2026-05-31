# Example Mod

这是一个基于 Fabric 的 Minecraft 1.21.11 模组，当前主要内容是强化版水怪和 TNT Fishing Rod。

## 功能

- 新增水怪实体，会在主世界水域中生成。
- 水怪会主动攻击生存玩家，不再因为上岸而窒息。
- 水怪会模仿附近玩家装备，并根据战斗情况使用剑、斧、盾牌、不死图腾和治疗物资。
- 水怪在平地会优先使用剑和斧近战。
- 水怪会使用 TNT Fishing Rod 召唤多层圆环 TNT。
- TNT Fishing Rod 右键召唤从空中下落的多层圆环 TNT。
- TNT Fishing Rod 左键方块可切换 TNT 是否破坏方块。
- 水怪死亡后会掉落 TNT Fishing Rod。
- 自定义 TNT 默认不会破坏方块，开启破坏模式后才会破坏地形。
- 水怪生成数量已控制在较合理范围。

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

- `src/main/java/com/example/examplemod/ExampleMod.java`：物品、实体、生成规则和事件注册。
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
