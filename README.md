# From Cell to Stars

A Minecraft (Fabric 1.21.1) mod about evolution: you start as a tiny cell in the primordial soup,
eat, grow and evolve - with the long-term goal of going from a cell all the way to the stars.
Inspired by the stages of *Spore*; this is a fan project and is not affiliated with EA or Maxis.

Built together with Claude (Anthropic's AI) - I design, playtest and steer, Claude writes most of the code.

## What's in it (cell stage)

- Top-down camera over a deep, murky primordial soup dimension
- Your own cell: editor with body parts (flagella, spikes, membrane), mouth / diet, colours and patterns
- Mating call -> editor -> hatching from an egg; first hatch arrives on a meteor
- Growth in small spurts, the soup changes as you grow
- 27 AI species with fixed sizes and their own temperaments - you outgrow them like in Spore
- Spore-style combat: damage depends on the size relation; jaws, spikes and a latching proboscis
- Diet path (herbivore / omnivore / carnivore) that decides your creature-stage ability
- Spore-style HUD: DNA, evolution bar split into stages, portrait with health

## Running it

Windows: double-click `run.bat`. It finds a suitable Java (21-24) or downloads Java 21 into `jdk/`
the first time, builds the mod and starts a test Minecraft. `run-shaders.bat` does the same with
Sodium + Iris for shader packs (put packs into `run/shaderpacks`). `update.bat` pulls the latest code first.

Test commands (in the soup): `/evo dna <n>`, `/evo level <1-5>`, `/evo spawn <species>`, `/evo food`,
`/evo heal`, `/evo mate`, `/evo editor`, `/evo evolve`, `/evo clear`.
