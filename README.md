# Mammoth: Horizontally scalable Minecraft server
### Warning: this README is very outdated and will be updated ASAP
### A WorldQL plugin for Spigot-based servers

## What is WorldQL?
WorldQL is a 3D database that's fast enough to use as a game server. Similarly to how SQL allow you to easily insert and query tabular data, WorldQL provides a simple interface to create and track 3D objects.

The benefit of using WorldQL for your next multiplayer game development project is enormous. Save dozens of hours by starting with a player modifiable 3D world with optional persistence. Be sure to [join our Discord community](https://discord.gg/tDZkXQPzEw).

## What is Mammoth?
Mammoth uses WorldQL to scale a single Minecraft world across multiple server processes. Running multiple Minecraft server processes allows for better core utilization and allows for thousands of players to enjoy a single world. A collection of Minecraft servers using this plugin to sync with a WorldQL server is called a *Mammoth cluster*.

Mammoth has a number of features:

1. It brings dynamic autoscaling to a Minecraft server. Cluster servers can be created on demand and they'll sync the world from WorldQL as players request it.
   1. WorldQL stores all block modifications from the base seed in its database.
   2. Cluster Minecraft servers are ephemeral and don't need to store their own authoritative copy of the world.
2. Players on different servers can see and punch each-other.
   1. WorldQL allows for efficient message-passing between server nodes.
   2. Each Minecraft server continuously reports its player positions to WorldQL. 
   3. WorldQL then pushes player positions to the right servers based on which players are near one-another. The Mammoth plugin receives these messages and sends player packets to Minecraft clients.
3. Block states instantly sync between worlds. Even weird stuff, like nether portals, signs, doors, beds, and glass panes.
4. Chests maintain a correct inventory no matter where they're accessed from. 

5. Redstone support is coming soon!

## How to build this plugin and use Mammoth yourself!
I recommend using IntelliJ IDEA and following the guides on the Spigot website to set up your Java 16 environment.

Then just clone this repo, open it in IntelliJ, and use the maven "package" lifecycle action to build the plugin. It's required to run [Spigot BuildTools](https://www.spigotmc.org/wiki/buildtools/) first to install a 1.18 jar in your local maven repo.
Also, you'll need to create a PostgreSQL database:

```sql
CREATE DATABASE worldql;
CREATE USER dbuser_worldql WITH PASSWORD 'worldql';
GRANT ALL PRIVILEGES ON DATABASE worldql TO dbuser_worldql;
```


## History of Mammoth
- March 2020: Inspired by large Minecraft events (like Square Garden) and 2b2t, the original goal for horizontally scaling a Minecraft server by splitting the world was realized. I wanted a quarantine project, so I started working on it.
- April 2020: The first version of Mammoth was deployed and played by about 200 people. Game servers would "claim" chunks via a redis server entry. When a player moved between servers, there would be a visible disconnect+reconnect delay. This was overly complicated and is detailed in [this Google Doc](https://docs.google.com/document/d/1jeIg34jGNuWTFUftrGySZa6M6S_VXok0J0-qqCBpJfw/edit?usp=sharing).
- May 2020: I shut the server down and did some analysis on the world files and how optimally the world was split between nodes (not very). The software had a ton of bugs and could be abused to infinitely duplicate mobs (even the Ender Dragon). I also realized that I didn't need a complex world claiming system, and could simply divide the world via a simple mathematical formula.
- Jun 2020: Released the second version of Mammoth which used a procedural world splitting technique.
- August 2020: SalC1 ran a server crash stream to test the software. I had a small lobby server designed to forward players to the correct cluster server, which immediately crashed. Regardless, over 600 people were able to connect over the course of the night and the project gained a lot of publicity.
- September 2020 - July 2021: Conception and initial development of WorldQL. A complete rewrite of Mammoth to use it.






