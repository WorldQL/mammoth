package com.worldql.client.serialization;

public enum Replication {
    ExceptSelf(com.worldql.client.Messages.Replication.ExceptSelf),
    IncludingSelf(com.worldql.client.Messages.Replication.IncludingSelf),
    OnlySelf(com.worldql.client.Messages.Replication.OnlySelf);

    private final int value;

    Replication(int value) {
        this.value = value;
    }

    public static Replication fromValue(int value) {
        return switch (value) {
            case com.worldql.client.Messages.Replication.IncludingSelf -> Replication.IncludingSelf;
            case com.worldql.client.Messages.Replication.OnlySelf -> Replication.OnlySelf;

            default -> Replication.ExceptSelf;
        };
    }

    public int getValue() {
        return value;
    }
}
