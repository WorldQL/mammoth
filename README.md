> 🪦 This project is no longer maintained. Check out [MultiPaper](https://github.com/PureGero/MultiPaper).

# Mammoth: Horizontally scalable Minecraft server
### A Spigot plugin demonstrating WorldQL's database and message broker.

## What is Mammoth?
Mammoth uses WorldQL to scale a single Minecraft world across multiple server processes. Running multiple Minecraft server processes allows for better core utilization and allows for more players to enjoy a single world. A collection of Minecraft servers using this plugin to sync with a WorldQL server is called a *Mammoth cluster*.

Mammoth has two modes:
- **Sliced mode**: The world is divided on server boundaries. Areas near server boundaries are synchronized. When a player crosses a server boundary they are transferred alonside their inventory, effects, any ridden mobs, or nearby villagers.
- **Seamless mode** (experimental): The world is not divided any block changes and player movement are synced.

## How to use Mammoth

1. Set up WorldQL. Download the latest release for Linux from https://github.com/WorldQL/worldql_server/actions and extract it into a folder.
   1. Set up a database using the commands below (change the password):
    ```sql
       CREATE DATABASE worldql;
       CREATE USER dbuser_worldql WITH PASSWORD 'example';
       GRANT ALL PRIVILEGES ON DATABASE worldql TO dbuser_worldql;
   ```
   3. Create a .env file with the following contents:
   ```
    WQL_SUBSCRIPTION_REGION_CUBE_SIZE=16
    WQL_POSTGRES_CONNECTION_STRING="host=localhost dbname=worldql user=dbuser_worldql password=worldql"
   ```
2. Set up redis-server and run it on localhost.
3. Use https://github.com/WorldQL/mc_provisioner to create your Minecraft cluster. Download the latest version of the Mammoth plugin from https://github.com/WorldQL/mammoth/actions and place it in a plugins folder next to the provisioner executable.
4. Create the servers with `./provisioner init` and run them with `./provisioner start`.
5. The default config will be copied to all servers in the cluster. You can copy it into your top-level plugins folder and edit it to your desired configuration. Once you've made changes to your plugins folder, you can push it to all servers with `./provisioner stop && ./provisioner sync && ./provisioner start`.
6. Create a Velocity proxy server and add all your cluster servers to it with the names `mammoth_0`, `mammoth_1`, etc. You can change the server name prefix in config.yml.
7. Start the Velocity proxy and connect to it. It will be running in sliced mode by default with divisions every 100 blocks for demonstration purposes (you should set this to a value over 1000 for actual servers). Place blocks and cross a server border and watch everything sync.

## Features
- Cross-server TP.
  - /mtp teleports to another player regardless of their server by looking their position up across the cluster.
  - /mtpa sends an Essentials-style teleport request to a player.
  - /mtpaccept accepts a teleport request.


## History of Mammoth
- March 2020: Inspired by large Minecraft events (like Square Garden) and 2b2t, the original goal for horizontally scaling a Minecraft server by splitting the world was realized. I wanted a quarantine project, so I started working on it.
- April 2020: The first version of Mammoth was deployed and played by about 200 people. Game servers would "claim" chunks via a redis server entry. When a player moved between servers, there would be a visible disconnect+reconnect delay. This was overly complicated and is detailed in [this Google Doc](https://docs.google.com/document/d/1jeIg34jGNuWTFUftrGySZa6M6S_VXok0J0-qqCBpJfw/edit?usp=sharing).
- May 2020: I shut the server down and did some analysis on the world files and how optimally the world was split between nodes (not very). The software had a ton of bugs and could be abused to infinitely duplicate mobs (even the Ender Dragon). I also realized that I didn't need a complex world claiming system, and could simply divide the world via a simple mathematical formula.
- Jun 2020: Released the second version of Mammoth which used a procedural world splitting technique.
- August 2020: SalC1 ran a server crash stream to test the software. I had a small lobby server designed to forward players to the correct cluster server, which immediately crashed. Regardless, over 600 people were able to connect over the course of the night and the project gained a lot of publicity.
- September 2020 - July 2021: Conception and initial development of WorldQL. A complete rewrite of Mammoth to use it.






