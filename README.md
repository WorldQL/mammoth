# Mammoth Plugin v3 for Spigot Servers (WIP)
Mammoth is a horizontally scalable Minecraft server designed to allow 1,000s of players to enjoy a single world.

This plugin is designed to be used in conjunction with a WorldQL server to load balance a world between multiple Minecraft server processes. It allows for dynamic autoscaling (they can be killed and instantiated as you please), as game servers are ephemeral and don't need to store their own authoritative copy of the world.

### History of Mammoth
- March 2020: Inspired by large Minecraft events (like Square Garden) and 2b2t, the original goal for horizontally scaling a Minecraft server by splitting the world was realized. I wanted a quarantine project, so I started working on it.
- April 2020: The first version of Mammoth was deployed and played by about 200 people. Game servers would "claim" chunks via a redis server entry. When a player moved between servers, there would be a visible disconnect+reconnect delay. This was overly complicated and is detailed in [this Google Doc](https://docs.google.com/document/d/1jeIg34jGNuWTFUftrGySZa6M6S_VXok0J0-qqCBpJfw/edit?usp=sharing).
- May 2020: I shut the server down and did some analysis on the world files and how optimally the world was split between nodes (not very). The software had a ton of bugs and could be abused to infinitely duplicate mobs (even the Ender Dragon). I also realized that I didn't need a complex world claiming system, and could simply divide the world via a simple mathematical formula.
- Jun 2020: TODO: finish writing this.

