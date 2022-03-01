package com.worldql.mammoth.worldql_serialization;

public enum Instruction {
    Heartbeat(com.worldql.mammoth.Messages.Instruction.Heartbeat),
    Handshake(com.worldql.mammoth.Messages.Instruction.Handshake),
    PeerConnect(com.worldql.mammoth.Messages.Instruction.PeerConnect),
    PeerDisconnect(com.worldql.mammoth.Messages.Instruction.PeerDisconnect),
    AreaSubscribe(com.worldql.mammoth.Messages.Instruction.AreaSubscribe),
    AreaUnsubscribe(com.worldql.mammoth.Messages.Instruction.AreaUnsubscribe),
    GlobalMessage(com.worldql.mammoth.Messages.Instruction.GlobalMessage),
    LocalMessage(com.worldql.mammoth.Messages.Instruction.LocalMessage),
    RecordCreate(com.worldql.mammoth.Messages.Instruction.RecordCreate),
    RecordRead(com.worldql.mammoth.Messages.Instruction.RecordRead),
    RecordUpdate(com.worldql.mammoth.Messages.Instruction.RecordUpdate),
    RecordDelete(com.worldql.mammoth.Messages.Instruction.RecordDelete),
    RecordReply(com.worldql.mammoth.Messages.Instruction.RecordReply),
    Unknown(com.worldql.mammoth.Messages.Instruction.Unknown);

    private final int value;

    Instruction(int value) {
        this.value = value;
    }

    public static Instruction fromValue(int value) {
        return switch (value) {
            case com.worldql.mammoth.Messages.Instruction.Heartbeat -> Instruction.Heartbeat;
            case com.worldql.mammoth.Messages.Instruction.Handshake -> Instruction.Handshake;
            case com.worldql.mammoth.Messages.Instruction.PeerConnect -> Instruction.PeerConnect;
            case com.worldql.mammoth.Messages.Instruction.PeerDisconnect -> Instruction.PeerDisconnect;
            case com.worldql.mammoth.Messages.Instruction.AreaSubscribe -> Instruction.AreaSubscribe;
            case com.worldql.mammoth.Messages.Instruction.AreaUnsubscribe -> Instruction.AreaUnsubscribe;
            case com.worldql.mammoth.Messages.Instruction.GlobalMessage -> Instruction.GlobalMessage;
            case com.worldql.mammoth.Messages.Instruction.LocalMessage -> Instruction.LocalMessage;
            case com.worldql.mammoth.Messages.Instruction.RecordCreate -> Instruction.RecordCreate;
            case com.worldql.mammoth.Messages.Instruction.RecordRead -> Instruction.RecordRead;
            case com.worldql.mammoth.Messages.Instruction.RecordUpdate -> Instruction.RecordUpdate;
            case com.worldql.mammoth.Messages.Instruction.RecordDelete -> Instruction.RecordDelete;
            case com.worldql.mammoth.Messages.Instruction.RecordReply -> Instruction.RecordReply;
            default -> Instruction.Unknown;
        };
    }

    public int getValue() {
        return value;
    }
}
