package com.worldql.client.serialization;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Messages.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
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

        // TODO: Records and Entities

        int offset = com.worldql.client.Messages.Message.endMessage(flatBuilder);
        flatBuilder.finish(offset);

        return flatBuilder.sizedByteArray();
    }

    public static Message decodeMessage(@NotNull ByteBuffer buf) {
        com.worldql.client.Messages.Message raw = com.worldql.client.Messages.Message.getRootAsMessage(buf);

        Instruction instruction = Instruction.fromValue(raw.instruction());
        UUID senderUuid = UUID.fromString(raw.senderUuid());
        Vec3D position = new Vec3D(raw.position());

        // TODO: Records and Entities

        return new Message(instruction, senderUuid, raw.worldName(), position, null, null, raw.parameter(), raw.flexAsByteBuffer());
    }
}
