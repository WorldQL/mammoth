![Temporary "logo" stolen from Project Zulu](https://i.imgur.com/Iez9K9t.png)
# Mammoth: a horizontally scalable Minecraft server.
This repository contains all of the code that is responsible for:
- Syncing player data to/from redis.
- Transferring players based on their location or activity.

This plugin only needs to be installed on game servers, *not lobby servers*. It is recommended that you use Mammoth with a separate lobby server, but it is not required. However, if you choose not to use a lobby server, mammoth0 will be the effective lobby. This plugin requires a Waterfall proxy to work at all.

# This is incomplete! 
This code is *technically* enough to run a Mammoth load balanced world by yourself. However, it's much easier with the setup and management scripts, which allow for easy orchestration of the servers in your cluster. I am working on getting all the tools and scripts on GitHub ASAP, I hope to release a full tutorial on running your own Mammoth cluster next week.
