package com.chaosarena.model;

import lombok.Data;

@Data
public class Trap {

    public enum TrapType { BANANA, SPRING, GLUE }

    private final String id;
    private final TrapType type;
    private final String ownerId;

    private double x;
    private double y;
    private boolean active = true;

    private static final double TRIGGER_RADIUS = 24.0;

    public Trap(String id, TrapType type, String ownerId, double x, double y) {
        this.id = id;
        this.type = type;
        this.ownerId = ownerId;
        this.x = x;
        this.y = y;
    }

    public boolean isTriggeredBy(Player player) {
        if (!active) return false;
        if (player.getId().equals(ownerId)) return false;
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        return Math.sqrt(dx * dx + dy * dy) <= TRIGGER_RADIUS;
    }

    public void trigger() {
        this.active = false;
    }
}