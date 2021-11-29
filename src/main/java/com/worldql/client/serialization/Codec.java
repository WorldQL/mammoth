package com.worldql.client.serialization;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Messages.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Codec {
    private static final FlatBufferBuilder flatBuilder = new FlatBufferBuilder(1024);
    private static final FlexBuffersBuilder flexBuilder = new FlexBuffersBuilder();

    @NotNull
    public static FlexBuffersBuilder getFlexBuilder() {
        flexBuilder.clear();
        return flexBuilder;
    }

    public static byte[] encodeMessage(@NotNull Message message) {
        flatBuilder.clear();

        // region: Records
        int records = -1;
        if (message.records() != null) {
            int[] v = new int[message.records().size()];
            for (int i = 0; i < message.records().size(); i++) {
                Record record = message.records().get(i);

                int uuid = flatBuilder.createString(record.uuid().toString());
                int worldName = flatBuilder.createString(record.worldName());
                int data = flatBuilder.createString(record.data());

                int flex = -1;
                if (record.flex() != null) {
                    flex = flatBuilder.createByteVector(record.flex());
                }

                com.worldql.client.Messages.Record.startRecord(flatBuilder);
                com.worldql.client.Messages.Record.addUuid(flatBuilder, uuid);
                com.worldql.client.Messages.Record.addWorldName(flatBuilder, worldName);
                com.worldql.client.Messages.Record.addData(flatBuilder, data);

                if (record.position() != null) {
                    Vec3D position = record.position();
                    int pos = Vec3d.createVec3d(flatBuilder, position.x(), position.y(), position.z());

                    com.worldql.client.Messages.Record.addPosition(flatBuilder, pos);
                }

                if (flex != -1) {
                    com.worldql.client.Messages.Record.addFlex(flatBuilder, flex);
                }

                v[i] = com.worldql.client.Messages.Record.endRecord(flatBuilder);
            }

            records = com.worldql.client.Messages.Message.createRecordsVector(flatBuilder, v);
        }
        // endregion

        // region: Entities
        int entities = -1;
        if (message.entities() != null) {
            int[] v = new int[message.entities().size()];
            for (int i = 0; i < message.entities().size(); i++) {
                Entity entity = message.entities().get(i);

                int uuid = flatBuilder.createString(entity.uuid().toString());
                int worldName = flatBuilder.createString(entity.worldName());
                int data = flatBuilder.createString(entity.data());

                int flex = -1;
                if (entity.flex() != null) {
                    flex = flatBuilder.createByteVector(entity.flex());
                }

                com.worldql.client.Messages.Entity.startEntity(flatBuilder);
                com.worldql.client.Messages.Entity.addUuid(flatBuilder, uuid);
                com.worldql.client.Messages.Entity.addWorldName(flatBuilder, worldName);
                com.worldql.client.Messages.Entity.addData(flatBuilder, data);

                if (entity.position() != null) {
                    Vec3D position = entity.position();
                    int pos = Vec3d.createVec3d(flatBuilder, position.x(), position.y(), position.z());

                    com.worldql.client.Messages.Entity.addPosition(flatBuilder, pos);
                }

                if (flex != -1) {
                    com.worldql.client.Messages.Entity.addFlex(flatBuilder, flex);
                }

                v[i] = com.worldql.client.Messages.Entity.endEntity(flatBuilder);
            }

            entities = com.worldql.client.Messages.Message.createEntitiesVector(flatBuilder, v);
        }
        // endregion

        int uuid = flatBuilder.createString(message.senderUuid().toString());
        int worldName = flatBuilder.createString(message.worldName());

        int parameter = -1;
        if (message.parameter() != null) {
            parameter = flatBuilder.createString(message.parameter());
        }

        int flex = -1;
        if (message.flex() != null) {
            flex = flatBuilder.createByteVector(message.flex());
        }

        com.worldql.client.Messages.Message.startMessage(flatBuilder);
        com.worldql.client.Messages.Message.addInstruction(flatBuilder, message.instruction().getValue());
        com.worldql.client.Messages.Message.addSenderUuid(flatBuilder, uuid);
        com.worldql.client.Messages.Message.addWorldName(flatBuilder, worldName);
        com.worldql.client.Messages.Message.addReplication(flatBuilder, message.replication().getValue());

        if (message.position() != null) {
            Vec3D position = message.position();
            int pos = Vec3d.createVec3d(flatBuilder, position.x(), position.y(), position.z());

            com.worldql.client.Messages.Message.addPosition(flatBuilder, pos);
        }

        if (parameter != -1) {
            com.worldql.client.Messages.Message.addParameter(flatBuilder, parameter);
        }

        if (flex != -1) {
            com.worldql.client.Messages.Message.addFlex(flatBuilder, flex);
        }

        if (records != -1) {
            com.worldql.client.Messages.Message.addRecords(flatBuilder, records);
        }

        if (entities != -1) {
            com.worldql.client.Messages.Message.addEntities(flatBuilder, entities);
        }

        int offset = com.worldql.client.Messages.Message.endMessage(flatBuilder);
        flatBuilder.finish(offset);

        return flatBuilder.sizedByteArray();
    }

    public static Message decodeMessage(@NotNull ByteBuffer buf) {
        com.worldql.client.Messages.Message raw = com.worldql.client.Messages.Message.getRootAsMessage(buf);

        Instruction instruction = Instruction.fromValue(raw.instruction());
        UUID senderUuid = UUID.fromString(raw.senderUuid());
        Replication replication = Replication.fromValue(raw.replication());
        Vec3D position = raw.position() == null ? null : new Vec3D(raw.position());

        List<Record> recordList = new ArrayList<>();
        for (int i = 0; i < raw.recordsLength(); i++) {
            com.worldql.client.Messages.Record r = raw.records(i);
            Record decodedRecord = new Record(
                    UUID.fromString(r.uuid()),
                    new Vec3D(r.position()),
                    r.worldName(),
                    r.data(),
                    r.flexAsByteBuffer()
            );

            recordList.add(decodedRecord);
        }

        List<Entity> entityList = new ArrayList<>();
        for (int i = 0; i < raw.entitiesLength(); i++) {
            com.worldql.client.Messages.Entity e = raw.entities(i);
            Entity decodedEntity = new Entity(
                    UUID.fromString(e.uuid()),
                    new Vec3D(e.position()),
                    e.worldName(),
                    e.data(),
                    e.flexAsByteBuffer()
            );

            entityList.add(decodedEntity);
        }

        return new Message(
                instruction,
                senderUuid,
                raw.worldName(),
                replication,
                position,
                recordList,
                entityList,
                raw.parameter(),
                raw.flexAsByteBuffer()
        );
    }
}
