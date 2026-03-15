package com.chaosarena.model;

import lombok.Data;

@Data
public class Structure {

    public enum StructureType { WALL, JUMP_PAD, TURRET }

    private final String id;
    private final StructureType type;
    private final String ownerId;

    private double x;
    private double y;
    private double width = 40;
    private double height = 40;
    private int durability;

    public Structure(String id, StructureType type, String ownerId, double x, double y) {
        this.id = id;
        this.type = type;
        this.ownerId = ownerId;
        this.x = x;
        this.y = y;
        this.durability = switch (type) {
            case WALL     -> 100;
            case JUMP_PAD -> 150;
            case TURRET   -> 80;
        };
    }

    public void takeDamage(int damage) {
        this.durability = Math.max(0, this.durability - damage);
    }

    public boolean isDestroyed() {
        return durability <= 0;
    }

    public boolean collidesWith(double px, double py, double radius) {
        double closestX = Math.max(x, Math.min(px, x + width));
        double closestY = Math.max(y, Math.min(py, y + height));
        double dx = px - closestX;
        double dy = py - closestY;
        return (dx * dx + dy * dy) <= (radius * radius);
    }
}