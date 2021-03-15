# Mammoth (WorldQLClient) plugin overview

### ghost/
This folder contains logic related to creating player "ghosts" (players from other servers).


## On port useage

The first server uses port 29900 and each successive server will count up until they find an available port.
Addresses with port numbers are stored in the central redis directory of Mammoth game servers.

## To compile protobuf
(from the  com/worldql/client folder)
```shell
protoc -I=protobuf --java_out=../../../. protobuf/*.proto
```