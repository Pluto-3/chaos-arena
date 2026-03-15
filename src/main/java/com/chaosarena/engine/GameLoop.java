package com.chaosarena.engine;

import com.chaosarena.model.ArenaMap;
import com.chaosarena.model.GameState;
import com.chaosarena.model.Player;
import com.chaosarena.room.Room;
import com.chaosarena.room.RoomManager;
import com.chaosarena.websocket.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
public class GameLoop {

    private static final Logger log = LoggerFactory.getLogger(GameLoop.class);
    private static final int RESTART_DELAY_TICKS = 100;

    private final RoomManager roomManager;
    private final GameEngine gameEngine;
    private final MessageSender messageSender;
    private final ConcurrentHashMap<String, Integer> restartCountdown = new ConcurrentHashMap<>();

    public GameLoop(RoomManager roomManager, GameEngine gameEngine, MessageSender messageSender) {
        this.roomManager   = roomManager;
        this.gameEngine    = gameEngine;
        this.messageSender = messageSender;
    }

    @Scheduled(fixedRate = 50)
    public void tick() {
        roomManager.getAllRooms().forEach(room -> {
            try {
                tickRoom(room);
            } catch (Exception e) {
                log.error("Error ticking room {}: {}", room.getCode(), e.getMessage(), e);
            }
        });
    }

    private void tickRoom(Room room) {
        GameState state = room.getGameState();

        switch (state.getPhase()) {
            case WAITING -> {
                if (room.playerCount() >= 2) {
                    state.resetRound();
                    messageSender.broadcastToRoom(room, messageSender.buildStartMessage(room));
                    log.info("Room {} — round started with {} players", room.getCode(), room.playerCount());
                }
            }
            case ACTIVE -> {
                List<GameEvent> events = gameEngine.tick(room);
                messageSender.broadcastToRoom(room, messageSender.buildStateUpdate(room));
                for (GameEvent event : events) {
                    messageSender.broadcastToRoom(room, messageSender.buildEventMessage(event));
                }
            }
            case ROUND_OVER -> {
                int countdown = restartCountdown.merge(room.getCode(), 1, Integer::sum);
                if (countdown == 1) {
                    messageSender.broadcastToRoom(room, messageSender.buildRoundEndMessage(room));
                }
                if (countdown >= RESTART_DELAY_TICKS) {
                    restartCountdown.remove(room.getCode());
                    respawnAllPlayers(room);
                    state.resetRound();
                    messageSender.broadcastToRoom(room, messageSender.buildStartMessage(room));
                    log.info("Room {} — round restarted", room.getCode());
                }
            }
        }
    }

    private void respawnAllPlayers(Room room) {
        List<Player> players = room.getGameState().getPlayers().values().stream().toList();
        double[][] spawns = ArenaMap.SPAWN_POINTS;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            double[] spawn = spawns[i % spawns.length];
            p.setX(spawn[0]);
            p.setY(spawn[1]);
            p.setHealth(100);
            p.setAlive(true);
            p.setMoveDirection("NONE");
            p.setGlued(false);
            p.setGluedTicksRemaining(0);
            p.setLaunchVelocityX(0);
            p.setLaunchVelocityY(0);
        }
    }
}