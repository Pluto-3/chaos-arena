package com.chaosarena.model;

import lombok.Data;

@Data
public class Player {

    private final String id;
    private String name;

    private double x;
    private double y;

    private int health = 100;
    private boolean alive = true;

    private String moveDirection = "NONE";
    private double speed = 5.0;
    private String weapon = "frying_pan";

    private boolean glued = false;
    private int gluedTicksRemaining = 0;
    private double launchVelocityX = 0;
    private double launchVelocityY = 0;

    public Player(String id, String name, double startX, double startY) {
        this.id = id;
        this.name = name;
        this.x = startX;
        this.y = startY;
    }

    public void takeDamage(int damage) {
        this.health = Math.max(0, this.health - damage);
        if (this.health <= 0) this.alive = false;
    }

    public void applyGlue(int durationTicks) {
        this.glued = true;
        this.gluedTicksRemaining = durationTicks;
    }

    public void applyLaunch(double vx, double vy) {
        this.launchVelocityX = vx;
        this.launchVelocityY = vy;
    }

    public void tickEffects() {
        if (glued) {
            gluedTicksRemaining--;
            if (gluedTicksRemaining <= 0) glued = false;
        }
        if (launchVelocityX != 0 || launchVelocityY != 0) {
            x += launchVelocityX;
            y += launchVelocityY;
            launchVelocityX *= 0.8;
            launchVelocityY *= 0.8;
            if (Math.abs(launchVelocityX) < 0.1) launchVelocityX = 0;
            if (Math.abs(launchVelocityY) < 0.1) launchVelocityY = 0;
        }
    }

    public boolean isLaunching() {
        return launchVelocityX != 0 || launchVelocityY != 0;
    }
}