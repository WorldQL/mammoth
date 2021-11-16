package com.worldql.client;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.Messages.Message;
import com.worldql.client.Messages.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public final class MessageCodec {
    private static final FlatBufferBuilder flatBuilder = new FlatBufferBuilder(1024);
    private static final FlexBuffersBuilder flexBuilder = new FlexBuffersBuilder();

    @NotNull
    public static FlexBuffersBuilder getFlexBuilder() {
        flexBuilder.clear();
        return flexBuilder;
    }

    public static class Vec3D {
        private final float x;
        private final float y;
        private final float z;

        public Vec3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static byte[] encodeMessage(@NotNull String uuid, @NotNull int instruction, @NotNull String worldName, @Nullable Vec3D position, @Nullable String parameter, @Nullable ByteBuffer flex) {
        flatBuilder.clear();

        int sender_uuid = flatBuilder.createString(uuid);
        int world_name = flatBuilder.createString(worldName);

        int param = -1;
        if (parameter != null) {
            param = flatBuilder.createString(parameter);
        }

        int flex_offset = -1;
        if (flex != null) {
            flex_offset = flatBuilder.createByteVector(flex);
        }

        Message.startMessage(flatBuilder);
        Message.addInstruction(flatBuilder, instruction);
        Message.addSenderUuid(flatBuilder, sender_uuid);
        Message.addWorldName(flatBuilder, world_name);

        if (position != null) {
            int pos = Vec3d.createVec3d(flatBuilder, position.x, position.y, position.z);
            Message.addPosition(flatBuilder, pos);
        }

        if (param != -1) {
            Message.addParameter(flatBuilder, param);
        }

        if (flex_offset != -1) {
            Message.addFlex(flatBuilder, flex_offset);
        }

        int message = Message.endMessage(flatBuilder);
        flatBuilder.finish(message);

        return flatBuilder.sizedByteArray();
    }

    public static Message decodeMessage(ByteBuffer buf) {
        return Message.getRootAsMessage(buf);
    }
}
