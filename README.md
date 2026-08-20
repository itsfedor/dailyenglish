# DailyEnglish

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![PaperMC](https://img.shields.io/badge/PaperMC-1.21-4ade80)]()
[![Vault](https://img.shields.io/badge/Vault-required-8250df)]()

Daily English tasks inside Minecraft. Each level has its own task pool
(craft a brewing stand, plant 32 carrots, collect 8 coal) split into easy,
medium, and hard difficulty. Players get their task list for the day, work
through it, and earn rewards.

## Why this plugin exists

A student who logs in every day to do one small English task is learning.
The tasks double as gameplay goals, so the language practice is a side
effect of playing, not homework.

## Features

- Level-tuned task pools: A0 tasks teach basics, B1 tasks demand real recipes (conduits, respawn anchors, lodestone compasses)
- Task types: CRAFT, PLANT, COLLECT and more, each with a written English instruction
- Book GUI shows today's tasks in-game
- Rewards paid through Vault

## Requirements

- Paper or Spigot 1.21+
- Vault with an economy plugin
- Optional: LuckPerms for per-level task pools

## Commands

```
/tasks                        show your daily tasks
/dailyenglish answer <taskId> <index>   submit an answer
```

## Install

1. Put `DailyEnglish.jar` in `plugins/`.
2. Copy `config.example.yml` to `config.yml`.
3. Copy the `tasks/` folder next to `config.yml`.
4. Restart the server.

## Task files

Task pools live in `plugins/DailyEnglish/tasks/`, one file per level
(`tasks_a1.yml`, `tasks_b1.yml`, ...). Tasks are plain YAML entries:

```yaml
- {id: b1_craft_01, type: CRAFT, difficulty: MEDIUM,
   instruction: "Craft a brewing stand", target: BREWING_STAND}
```

The full task sets for every level ship in the `tasks/` folder of this repo.

## License

MIT. See [LICENSE](LICENSE).
