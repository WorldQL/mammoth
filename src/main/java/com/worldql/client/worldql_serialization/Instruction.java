package com.worldql.client.worldql_serialization;

public enum Instruction {
    Heartbeat(com.worldql.client.Messages.Instruction.Heartbeat),
    Handshake(com.worldql.client.Messages.Instruction.Handshake),
    PeerConnect(com.worldql.client.Messages.Instruction.PeerConnect),
    PeerDisconnect(com.worldql.client.Messages.Instruction.PeerDisconnect),
    AreaSubscribe(com.worldql.client.Messages.Instruction.AreaSubscribe),
    AreaUnsubscribe(com.worldql.client.Messages.Instruction.AreaUnsubscribe),
    GlobalMessage(com.worldql.client.Messages.Instruction.GlobalMessage),
    LocalMessage(com.worldql.client.Messages.Instruction.LocalMessage),
    RecordCreate(com.worldql.client.Messages.Instruction.RecordCreate),
    RecordRead(com.worldql.client.Messages.Instruction.RecordRead),
    RecordUpdate(com.worldql.client.Messages.Instruction.RecordUpdate),
    RecordDelete(com.worldql.client.Messages.Instruction.RecordDelete),
    RecordReply(com.worldql.client.Messages.Instruction.RecordReply),
    Unknown(com.worldql.client.Messages.Instruction.Unknown);

    private final int value;

    Instruction(int value) {
        this.value = value;
    }

    public static Instruction fromValue(int value) {
        return switch (value) {
            case com.worldql.client.Messages.Instruction.Heartbeat -> Instruction.Heartbeat;
            case com.worldql.client.Messages.Instruction.Handshake -> Instruction.Handshake;
            case com.worldql.client.Messages.Instruction.PeerConnect -> Instruction.PeerConnect;
            case com.worldql.client.Messages.Instruction.PeerDisconnect -> Instruction.PeerDisconnect;
            case com.worldql.client.Messages.Instruction.AreaSubscribe -> Instruction.AreaSubscribe;
            case com.worldql.client.Messages.Instruction.AreaUnsubscribe -> Instruction.AreaUnsubscribe;
            case com.worldql.client.Messages.Instruction.GlobalMessage -> Instruction.GlobalMessage;
            case com.worldql.client.Messages.Instruction.LocalMessage -> Instruction.LocalMessage;
            case com.worldql.client.Messages.Instruction.RecordCreate -> Instruction.RecordCreate;
            case com.worldql.client.Messages.Instruction.RecordRead -> Instruction.RecordRead;
            case com.worldql.client.Messages.Instruction.RecordUpdate -> Instruction.RecordUpdate;
            case com.worldql.client.Messages.Instruction.RecordDelete -> Instruction.RecordDelete;
            case com.worldql.client.Messages.Instruction.RecordReply -> Instruction.RecordReply;
            default -> Instruction.Unknown;
        };
    }

    public int getValue() {
        return value;
    }
}
