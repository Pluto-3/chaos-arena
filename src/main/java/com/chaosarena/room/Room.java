package com.chaosarena.room;

import com.chaosarena.model.GameState;
import com.chaosarena.model.Player;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Room {

    private final String code;
    private final GameState gameState = new GameState();

    private final Map<String, String> sessionToPlayer = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public Room(String code) {
        this.code = code;
    }

    public void addSession(WebSocketSession session, Player player) {
        sessions.put(session.getId(), session);
        sessionToPlayer.put(session.getId(), player.getId());
        gameState.addPlayer(player);
    }

    public void removeSession(String sessionId) {
        String playerId = sessionToPlayer.remove(sessionId);
        if (playerId != null) gameState.removePlayer(playerId);
        sessions.remove(sessionId);
    }

    public Player getPlayerBySession(String sessionId) {
        String playerId = sessionToPlayer.get(sessionId);
        return playerId != null ? gameState.getPlayer(playerId) : null;
    }

    public Collection<WebSocketSession> getSessions() {
        return sessions.values();
    }

    public int playerCount() {
        return sessions.size();
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }
}