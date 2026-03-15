package com.chaosarena.websocket;

import com.chaosarena.engine.GameEvent;
import com.chaosarena.model.ArenaMap;
import com.chaosarena.model.GameState;
import com.chaosarena.room.Room;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

@Component
public class MessageSender {

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public void broadcastToRoom(Room room, Map<String, Object> payload) {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Serialisation error: {}", e.getMessage());
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : room.getSessions()) {
            sendRaw(session, message);
        }
    }

    public void sendToSession(WebSocketSession session, Map<String, Object> payload) {
        try {
            sendRaw(session, new TextMessage(mapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.warn("Could not send to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendRaw(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) session.sendMessage(message);
        } catch (Exception e) {
            log.warn("Could not send to session {}: {}", session.getId(), e.getMessage());
        }
    }

    public Map<String, Object> buildStateUpdate(Room room) {
        GameState state = room.getGameState();
        return Map.of(
                "type",       "STATE_UPDATE",
                "tick",       state.getTick(),
                "players",    state.getPlayers().values(),
                "structures", state.getStructures(),
                "traps",      state.getTraps(),
                "timeLeft",   state.getRoundTicksRemaining()
        );
    }

    public Map<String, Object> buildEventMessage(GameEvent event) {
        return Map.of(
                "type",     event.getType().name(),
                "entityId", event.getEntityId() != null ? event.getEntityId() : "",
                "message",  event.getMessage()
        );
    }

    public Map<String, Object> buildRoundEndMessage(Room room) {
        GameState state = room.getGameState();
        String winner = state.getLastWinner() != null
                ? state.getLastWinner()
                : "Nobody (draw!)";
        return Map.of(
                "type",     "ROUND_END",
                "winner",   winner,
                "messages", state.getDeathMessages()
        );
    }

    public Map<String, Object> buildStartMessage(Room room) {
        return Map.of(
                "type",        "ROUND_START",
                "playerCount", room.playerCount(),
                "mapWidth",    ArenaMap.WIDTH,
                "mapHeight",   ArenaMap.HEIGHT,
                "staticWalls", ArenaMap.getStaticWalls()
        );
    }

    public Map<String, Object> buildJoinAck(Room room, String playerId) {
        return Map.of(
                "type",        "JOIN_ACK",
                "playerId",    playerId,
                "roomCode",    room.getCode(),
                "mapWidth",    ArenaMap.WIDTH,
                "mapHeight",   ArenaMap.HEIGHT,
                "staticWalls", ArenaMap.getStaticWalls()
        );
    }

    public Map<String, Object> buildError(String message) {
        return Map.of("type", "ERROR", "message", message);
    }
}