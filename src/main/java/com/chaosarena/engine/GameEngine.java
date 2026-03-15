package com.chaosarena.engine;

import com.chaosarena.model.*;
import com.chaosarena.room.Room;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GameEngine {

    private static final double PLAYER_RADIUS = 16.0;
    private static final int ATTACK_RANGE = 60;
    private static final List<ArenaMap.StaticWall> STATIC_WALLS = ArenaMap.getStaticWalls();

    public List<GameEvent> tick(Room room) {
        GameState state = room.getGameState();
        List<GameEvent> events = new ArrayList<>();

        if (state.getPhase() != GameState.Phase.ACTIVE) return events;

        state.setTick(state.getTick() + 1);

        for (Player player : state.getPlayers().values()) {
            if (!player.isAlive()) continue;
            tickEffectsAndMove(player, state);
        }

        checkTraps(state, events);

        state.setRoundTicksRemaining(state.getRoundTicksRemaining() - 1);

        checkWinCondition(state, events);

        return events;
    }

    private void tickEffectsAndMove(Player player, GameState state) {
        player.tickEffects();

        if (player.isLaunching()) {
            clampToArena(player);
            return;
        }

        double speed = player.isGlued() ? player.getSpeed() * 0.35 : player.getSpeed();

        double dx = 0, dy = 0;
        switch (player.getMoveDirection()) {
            case "UP"         -> dy = -speed;
            case "DOWN"       -> dy =  speed;
            case "LEFT"       -> dx = -speed;
            case "RIGHT"      -> dx =  speed;
            case "UP_LEFT"    -> { dx = -speed * 0.707; dy = -speed * 0.707; }
            case "UP_RIGHT"   -> { dx =  speed * 0.707; dy = -speed * 0.707; }
            case "DOWN_LEFT"  -> { dx = -speed * 0.707; dy =  speed * 0.707; }
            case "DOWN_RIGHT" -> { dx =  speed * 0.707; dy =  speed * 0.707; }
            default           -> {}
        }

        double newX = player.getX() + dx;
        double newY = player.getY() + dy;

        if (!collidesWithWalls(newX, player.getY(), state)) player.setX(newX);
        if (!collidesWithWalls(player.getX(), newY, state)) player.setY(newY);

        clampToArena(player);
    }

    private boolean collidesWithWalls(double x, double y, GameState state) {
        for (ArenaMap.StaticWall wall : STATIC_WALLS) {
            if (wall.collidesWith(x, y, PLAYER_RADIUS)) return true;
        }
        for (Structure s : state.getStructures()) {
            if (!s.isDestroyed() && s.collidesWith(x, y, PLAYER_RADIUS)) return true;
        }
        return false;
    }

    private void clampToArena(Player player) {
        double margin = PLAYER_RADIUS + ArenaMap.TILE;
        player.setX(Math.max(margin, Math.min(ArenaMap.WIDTH - margin, player.getX())));
        player.setY(Math.max(margin, Math.min(ArenaMap.HEIGHT - margin, player.getY())));
    }

    public GameEvent processAttack(Room room, String attackerId, String targetId) {
        GameState state = room.getGameState();
        Player attacker = state.getPlayer(attackerId);
        Player target   = state.getPlayer(targetId);

        if (attacker == null || target == null) return null;
        if (!attacker.isAlive() || !target.isAlive()) return null;

        double dx = target.getX() - attacker.getX();
        double dy = target.getY() - attacker.getY();
        if (Math.sqrt(dx * dx + dy * dy) > ATTACK_RANGE) return null;

        int damage = switch (attacker.getWeapon()) {
            case "frying_pan"   -> 25;
            case "fish_slap"    -> 15;
            case "banana_throw" -> 10;
            default             -> 10;
        };

        target.takeDamage(damage);

        if (!target.isAlive()) {
            String msg = buildDeathMessage(attacker, target);
            state.getDeathMessages().add(msg);
            return new GameEvent(GameEvent.Type.PLAYER_DEAD, target.getId(), msg);
        }
        return null;
    }

    private void checkTraps(GameState state, List<GameEvent> events) {
        for (Trap trap : state.getTraps()) {
            if (!trap.isActive()) continue;
            for (Player player : state.getPlayers().values()) {
                if (!player.isAlive()) continue;
                if (trap.isTriggeredBy(player)) {
                    applyTrapEffect(trap, player);
                    trap.trigger();
                    String msg = buildTrapMessage(trap, player);
                    state.getDeathMessages().add(msg);
                    events.add(new GameEvent(GameEvent.Type.TRAP_TRIGGERED, player.getId(), msg));
                    break;
                }
            }
        }
    }

    private void applyTrapEffect(Trap trap, Player player) {
        switch (trap.getType()) {
            case BANANA -> {
                double vx = 0, vy = 0;
                switch (player.getMoveDirection()) {
                    case "UP"    -> vy = -8;
                    case "DOWN"  -> vy =  8;
                    case "LEFT"  -> vx = -8;
                    case "RIGHT" -> vx =  8;
                    default      -> vx =  8;
                }
                player.applyLaunch(vx, vy);
            }
            case SPRING -> {
                double dx = player.getX() - trap.getX();
                double dy = player.getY() - trap.getY();
                double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                player.applyLaunch((dx / dist) * 18, (dy / dist) * 18);
            }
            case GLUE -> player.applyGlue(60);
        }
    }

    private void checkWinCondition(GameState state, List<GameEvent> events) {
        List<Player> alive = state.alivePlayers();

        if (alive.size() == 1) {
            state.setLastWinner(alive.get(0).getName());
            state.setPhase(GameState.Phase.ROUND_OVER);
            events.add(new GameEvent(GameEvent.Type.ROUND_END, null,
                    alive.get(0).getName() + " wins!"));
            return;
        }
        if (alive.isEmpty() || state.getRoundTicksRemaining() <= 0) {
            state.setLastWinner(null);
            state.setPhase(GameState.Phase.ROUND_OVER);
            events.add(new GameEvent(GameEvent.Type.ROUND_END, null,
                    "Draw! Everyone's a loser."));
        }
    }

    private static final String[] WEAPON_VERBS = {
            "flattened", "obliterated", "pancaked", "yeeted", "destroyed"
    };

    private String buildDeathMessage(Player attacker, Player victim) {
        String verb = WEAPON_VERBS[new Random().nextInt(WEAPON_VERBS.length)];
        String suffix = switch (attacker.getWeapon()) {
            case "frying_pan"   -> "with a frying pan 🍳";
            case "fish_slap"    -> "with a wet fish 🐟";
            case "banana_throw" -> "with a suspiciously accurate banana 🍌";
            default             -> "somehow";
        };
        return attacker.getName() + " " + verb + " " + victim.getName() + " " + suffix;
    }

    private String buildTrapMessage(Trap trap, Player victim) {
        return switch (trap.getType()) {
            case BANANA -> victim.getName() + " slipped on a banana like a cartoon 🍌";
            case SPRING -> victim.getName() + " got launched into the stratosphere 🚀";
            case GLUE   -> victim.getName() + " is stuck in glue like an absolute clown 🤡";
        };
    }
}