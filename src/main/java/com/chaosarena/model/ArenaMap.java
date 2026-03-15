package com.chaosarena.model;

import java.util.ArrayList;
import java.util.List;

public class ArenaMap {

    public static final int WIDTH  = 800;
    public static final int HEIGHT = 600;
    public static final int TILE   = 40;

    public static final double[][] SPAWN_POINTS = {
            {80,  80},
            {680, 80},
            {80,  480},
            {680, 480},
            {400, 300},
    };

    public static List<StaticWall> getStaticWalls() {
        List<StaticWall> walls = new ArrayList<>();

        // Border
        walls.add(new StaticWall(0,   0,              WIDTH, TILE));
        walls.add(new StaticWall(0,   HEIGHT - TILE,  WIDTH, TILE));
        walls.add(new StaticWall(0,   0,              TILE,  HEIGHT));
        walls.add(new StaticWall(WIDTH - TILE, 0,     TILE,  HEIGHT));

        // Center cross
        walls.add(new StaticWall(340, 220, 120, 20));
        walls.add(new StaticWall(390, 160, 20,  80));
        walls.add(new StaticWall(390, 360, 20,  80));

        // Corner pillars
        walls.add(new StaticWall(160, 160, 40, 40));
        walls.add(new StaticWall(600, 160, 40, 40));
        walls.add(new StaticWall(160, 400, 40, 40));
        walls.add(new StaticWall(600, 400, 40, 40));

        // Side alcoves
        walls.add(new StaticWall(160, 260, 80, 20));
        walls.add(new StaticWall(160, 320, 80, 20));
        walls.add(new StaticWall(560, 260, 80, 20));
        walls.add(new StaticWall(560, 320, 80, 20));

        return walls;
    }

    public record StaticWall(double x, double y, double width, double height) {
        public boolean collidesWith(double px, double py, double radius) {
            double closestX = Math.max(x, Math.min(px, x + width));
            double closestY = Math.max(y, Math.min(py, y + height));
            double dx = px - closestX;
            double dy = py - closestY;
            return (dx * dx + dy * dy) <= (radius * radius);
        }
    }
}