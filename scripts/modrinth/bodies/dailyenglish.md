# DailyEnglish

Daily English tasks inside Minecraft. Each level has its own task pool
(craft a brewing stand, plant 32 carrots, collect 8 coal) split into easy,
medium, and hard difficulty. Players get their task list for the day, work
through it, and earn rewards.

Part of a gamified ESL teaching setup: chat is the lesson, money is the motivation. The other plugins live under the [ESL Automation Suite](https://github.com/itsfedor/esl-automation-suite).

## Requirements

- Vault (any Vault economy provider works, VaultUnlocked included)
- Optional: LuckPerms, for per-level task pools

## Features

- Level-tuned task pools: A0 tasks teach basics, B1 tasks demand real recipes (conduits, respawn anchors, lodestone compasses)
- Task types: CRAFT, PLANT, COLLECT and more, each with a written English instruction
- Book GUI shows today's tasks in-game
- Rewards paid through Vault

## Commands

```
/tasks                        show your daily tasks
/dailyenglish answer <taskId> <index>   submit an answer
```

## Task files

Task pools live in `plugins/DailyEnglish/tasks/`, one file per level
(`tasks_a1.yml`, `tasks_b1.yml`, ...). Tasks are plain YAML entries:

```yaml
- {id: b1_craft_01, type: CRAFT, difficulty: MEDIUM,
   instruction: "Craft a brewing stand", target: BREWING_STAND}
```

The full task files are in the [repo](https://github.com/itsfedor/dailyenglish/tree/main/tasks).

## License

MIT. Source code is in the [repository](https://github.com/itsfedor/dailyenglish).
