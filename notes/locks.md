Mammoth's technique for synchronizing servers can cause a variety of race conditions which can result in duplication glitches or worse. Here's how we address these issues:

### What happens if two players break the same block at the same time on different servers?
lolPants came up with a clever technique for preventing this by only allowing any given block to drop on a single server (the one it was broken on). Here's how it works:
1. When the player breaks a block, the MC server sends a LocalMessage to WorldQL with replication setting IncludingSelf.
2. This break event is sent to other servers, including the one it originated from.
3. When a server receives a LocalMessage about a block break that it caused, it drops the block.
You can see the implementation of this in PlayerBreakBlockListener and BlockTools.setRecord (for dropping the item).

### What happens when two players try to move items out of the same chest/shulker/whatever at the same time on different servers?
We lock inventories to a single player at once.

1. When an inventory is opened, the MC server acquires a lock to that specific container.
2. When an inventory is closed, the MC server releases that lock.

### What happens when redstone current changes in a block? How is redstone synced?
We use a strategy called "mark and sync" which follows this process:
1. Acquire a "redstone lock" on the chunk.
2. Redstone happens on the host MC server. It keeps an in-memory list of blocks that are effected by the redstone operation.
3. Every 1000ms, get the NBT data for every changed block and send a RecordCreate + LocalMessage with "MinecraftBlockUpdate".
4. The redstone lock is naturally released when the key expires.

### What happens with hoppers and other complex blocks?
Hoppers are locked as containers (two players cannot open them at the same time) and their InventoryMoveEvents are part of the same redstone lock. We treat redstone and non-player InventoryMoveEvents the same way.



We currently use redis for our locking system (since it supports key expiry) but soon this functionality for setting expiring keys for the purpose of locks will be built directly into WorldQL.
