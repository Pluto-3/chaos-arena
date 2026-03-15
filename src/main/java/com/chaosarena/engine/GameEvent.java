package com.chaosarena.engine;

import lombok.Data;

@Data
public class GameEvent {

    public enum Type {
        PLAYER_DEAD,
        TRAP_TRIGGERED,
        ROUND_END
    }

    private final Type type;
    private final String entityId;
    private final String message;
}