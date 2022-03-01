package com.worldql.mammoth.worldql_serialization;

public enum Replication {
    ExceptSelf(com.worldql.mammoth.Messages.Replication.ExceptSelf),
    IncludingSelf(com.worldql.mammoth.Messages.Replication.IncludingSelf),
    OnlySelf(com.worldql.mammoth.Messages.Replication.OnlySelf);

    private final int value;

    Replication(int value) {
        this.value = value;
    }

    public static Replication fromValue(int value) {
        return switch (value) {
            case com.worldql.mammoth.Messages.Replication.IncludingSelf -> Replication.IncludingSelf;
            case com.worldql.mammoth.Messages.Replication.OnlySelf -> Replication.OnlySelf;

            default -> Replication.ExceptSelf;
        };
    }

    public int getValue() {
        return value;
    }
}
