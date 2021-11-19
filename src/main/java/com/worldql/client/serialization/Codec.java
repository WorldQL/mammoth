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
                    flex = flatBuilder.createByteVector(message.flex());
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

        // TODO: Entities

        if (records != -1) {
            com.worldql.client.Messages.Message.addRecords(flatBuilder, records);
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

        // TODO: Entities

        List<Record> recordList = new ArrayList<>();
        for (int i = 0; i < raw.recordsLength(); i++) {
            com.worldql.client.Messages.Record r = raw.records(i);
            Record decodedRecord = new Record(UUID.fromString(r.uuid()),
                    new Vec3D(raw.position()), r.worldName(), r.data(), r.flexAsByteBuffer());
            recordList.add(decodedRecord);
        }


        return new Message(instruction, senderUuid, raw.worldName(), replication, position, recordList, null,
                raw.parameter(), raw.flexAsByteBuffer());
    }
}
