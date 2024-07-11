# Get Off My Lawn ReServed

*Get Off My Lawn ReServed* is a take on the popular concept of player claims for Survival/Freebuild Fabric servers. 
This mod works fully server side (no client mod required!) while being compatible with major Fabric modpacks

This project is a fork [Get Off My Lawn by Draylar](https://github.com/Draylar/get-off-my-lawn), with focus on improving and building on top of the original.


# Video Showcase

* English: https://youtu.be/R9-PuMRbNEc

* Polish: https://youtu.be/1V8kh0h3NoU

# Getting started

To get started, you'll have to craft a *Claim Anchor*. Each anchor has a different (configurable by admin) claim radius; after placing one, a box around it will be formed. This box is yours!

* **Makeshift**, default radius of 10
* **Reinforced**, default radius of 25
* **Glistening**, default radius of 50
* **Crystal**, default radius of 75
* **Emeradic**, default radius of 125
* **Withered**, default radius of 200

To see claim areas, you'll have to craft a *Goggles of (Claim) Revealing*

When this item equipped in the helmet, mainhand or offhand slot, claim outlines become visible.

## [Recipes](recipes.md)

## Claim configuration:
To configure your claim, you can interact with the anchor block. A UI will appear that offers several configuration options:
- The player list can be used to add and remove access of players to your claim
- The Augment list, that can be used for checking and configuring active augments

## Claim upgrades:
To upgrade your claim, place an Anchor Augment next to the core Claim Anchor. Anchor Augments available include:
- Ender Binding: Prevents Endermen from teleporting
- Villager Core: Prevents Zombies from damaging Villagers
- Greeter: MOTD to visitors
- Angelic Aura: Regen to all players inside region
- Withering Seal: Prevents wither status effect
- Force Field: non-whitelisted players get launched out of the claim
- Heaven's Wings: flight
- Lake Spirit's Grace: water breathing, water sight, and better breathing
- Chaos Zone: Strength to all players inside region
- PvP Arena: Allows changing pvp state in claim
- Explosion Controller: Allows toggling explosion protection

## Config:
You can find config file in `./config/getoffmylawn.json`. To reload it, just type `/goml admin reload` in chat/console.

```json5
{
  "makeshiftRadius": 10,                // Radius of makeshift claim
  "reinforcedRadius": 25,               // Radius of reinforced claim
  "glisteningRadius": 50,               // Radius of glistening claim
  "crystalRadius": 75,                  // Radius of crystal claim
  "emeradicRadius": 125,                // Radius of emeradic claim
  "witheredRadius": 200,                // Radius of withered claim
  "claimProtectsFullWorldHeight": false,// Makes claim protect area from bottom of the world to top
  "dimensionBlacklist": [               // Allows to blacklist specific dimensions
    "example:dim"
  ],             
  "regionBlacklist": {                  // Allows to blacklist specific regions
    "example:dim": [
      {
        x1: -200,
        y1: -64,
        z1: -200,
        x2: 200,
        y2: 512,
        z2: 200,
      }
    ]
  },
  "enabledAugments": {                  // Allows to enable/disable augments per their id
    "goml:lake_spirit_grace": true,
    "goml:angelic_aura": true,
    "goml:greeter": true,
    "goml:force_field": true,
    "goml:village_core": true,
    "goml:withering_seal": true,
    "goml:ender_binding": true,
    "goml:heaven_wings": true,
    "goml:chaos_zone": true
  },
  "allowedBlockInteraction": [          // Allows to interact with specific blocks in claim
    "somemod:store"
  ],
  "allowedEntityInteraction": [         // Allows to interact with specific entities in claim
    "minecraft:villager"
  ],
  "messagePrefix": "<dark_gray>[<#a1ff59>GOML</color>]", // Default prefix used in messages
  "placeholderNoClaimInfo": "<gray><italic>Wilderness",
  "placeholderNoClaimOwners": "<gray><italic>Nobody",
  "placeholderNoClaimTrusted": "<gray><italic>Nobody",
  "placeholderClaimCanBuildInfo": "${owners} <gray>(<green>${anchor}</green>)",
  "placeholderClaimCantBuildInfo": "${owners} <gray>(<red>${anchor}</red>)",
  "claimColorSource": "location"        // either "location" or "player" - "location" will chose the color based on the location of the claim (hash of coordinates), "player" will chose the color based on the owner of the claim (hash of UUID).
}
```


## License
*Get Off My Lawn ReServed* is available under the MIT license. The project, code, and assets found in this repository are available for free public use (as long as credited).
