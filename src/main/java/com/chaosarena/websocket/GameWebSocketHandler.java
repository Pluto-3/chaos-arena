package com.chaosarena.websocket;

import com.chaosarena.engine.GameEngine;
import com.chaosarena.engine.GameEvent;
import com.chaosarena.model.ArenaMap;
import com.chaosarena.model.Player;
import com.chaosarena.model.Structure;
import com.chaosarena.model.Trap;
import com.chaosarena.room.Room;
import com.chaosarena.room.RoomManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.chaosarena.model.Trap.TrapType;
import com.chaosarena.model.Structure.StructureType;

import java.util.*;

import com.chaosarena.model.*;
import com.chaosarena.room.Room;
import com.chaosarena.room.RoomManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.UUID;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final RoomManager roomManager;
    private final GameEngine gameEngine;
    private final MessageSender messageSender;
    private final ObjectMapper mapper = new ObjectMapper();

    public GameWebSocketHandler(RoomManager roomManager,
                                GameEngine gameEngine,
                                MessageSender messageSender) {
        this.roomManager   = roomManager;
        this.gameEngine    = gameEngine;
        this.messageSender = messageSender;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket disconnected: {}", session.getId());
        for (Room room : roomManager.getAllRooms()) {
            if (room.getPlayerBySession(session.getId()) != null) {
                room.removeSession(session.getId());
                roomManager.removeRoomIfEmpty(room.getCode());
                log.info("Player left room {}", room.getCode());
                return;
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode json = mapper.readTree(message.getPayload());
            String type = json.get("type").asText();
            switch (type) {
                case "JOIN_ROOM"  -> handleJoin(session, json);
                case "MOVE"       -> handleMove(session, json);
                case "ATTACK"     -> handleAttack(session, json);
                case "BUILD"      -> handleBuild(session, json);
                case "PLACE_TRAP" -> handlePlaceTrap(session, json);
                case "REACTION"   -> handleReaction(session, json);
                case "SWITCH_WEAPON" -> handleSwitchWeapon(session, json);
                default           -> log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
            messageSender.sendToSession(session, messageSender.buildError("Invalid message"));
        }
    }

    private void handleSwitchWeapon(WebSocketSession session, JsonNode json) {
        withPlayer(session, (room, player) -> {
            String weapon = json.get("weapon").asText();
            if (List.of("frying_pan", "fish_slap", "banana_throw").contains(weapon)) {
                player.setWeapon(weapon);
            }
        });
    }

    private void handleJoin(WebSocketSession session, JsonNode json) {
        String name     = json.get("name").asText("Stranger");
        String roomCode = json.get("roomCode").asText("").toUpperCase();

        Room room = roomManager.findRoom(roomCode).orElseGet(roomManager::createRoom);

        if (room.playerCount() >= 5) {
            messageSender.sendToSession(session, messageSender.buildError("Room is full"));
            return;
        }

        double[] spawn = ArenaMap.SPAWN_POINTS[room.playerCount() % ArenaMap.SPAWN_POINTS.length];
        Player player  = new Player(UUID.randomUUID().toString(), name, spawn[0], spawn[1]);
        room.addSession(session, player);

        messageSender.sendToSession(session, messageSender.buildJoinAck(room, player.getId()));
        log.info("Player '{}' joined room {} (id={})", name, room.getCode(), player.getId());
    }

    private void handleMove(WebSocketSession session, JsonNode json) {
        withPlayer(session, (room, player) -> {
            String dir = json.has("direction")
                    ? json.get("direction").asText("NONE").toUpperCase()
                    : "NONE";
            player.setMoveDirection(dir);
        });
    }

    private void handleAttack(WebSocketSession session, JsonNode json) {
        withPlayer(session, (room, player) -> {
            if (!player.isAlive()) return;
            String targetId = json.get("targetId").asText();
            GameEvent event = gameEngine.processAttack(room, player.getId(), targetId);
            if (event != null) {
                messageSender.broadcastToRoom(room, messageSender.buildEventMessage(event));
            }
        });
    }

    private void handleBuild(WebSocketSession session, JsonNode json) {
        withPlayer(session, (room, player) -> {
            if (!player.isAlive()) return;
            try {
                StructureType type = StructureType
                        .valueOf(json.get("structureType").asText().toUpperCase());
                double x = json.get("x").asDouble();
                double y = json.get("y").asDouble();
                room.getGameState().getStructures()
                        .add(new Structure(UUID.randomUUID().toString(), type, player.getId(), x, y));
            } catch (IllegalArgumentException ignored) {}
        });
    }

    private void handlePlaceTrap(WebSocketSession session, JsonNode json) {
        withPlayer(session, (room, player) -> {
            if (!player.isAlive()) return;
            try {
                TrapType type = TrapType
                        .valueOf(json.get("trapType").asText().toUpperCase());
                double x = json.get("x").asDouble();
                double y = json.get("y").asDouble();
                room.getGameState().getTraps()
                        .add(new Trap(UUID.randomUUID().toString(), type, player.getId(), x, y));
            } catch (IllegalArgumentException ignored) {}
        });
    }

    private void handleReaction(WebSocketSession session, JsonNode json) {
        withPlayer(session, (room, player) -> {
            String emoji = json.get("emoji").asText("😂");
            messageSender.broadcastToRoom(room, Map.of(
                    "type",       "REACTION",
                    "playerName", player.getName(),
                    "emoji",      emoji
            ));
        });
    }

    @FunctionalInterface
    interface PlayerAction {
        void execute(Room room, Player player);
    }

    private void withPlayer(WebSocketSession session, PlayerAction action) {
        for (Room room : roomManager.getAllRooms()) {
            Player player = room.getPlayerBySession(session.getId());
            if (player != null) {
                action.execute(room, player);
                return;
            }
        }
    }
}