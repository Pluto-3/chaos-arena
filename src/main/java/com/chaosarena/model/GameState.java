package com.chaosarena.model;

import lombok.Data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GameState {

    public enum Phase { WAITING, ACTIVE, ROUND_OVER }

    private Phase phase = Phase.WAITING;

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final List<Structure> structures  = Collections.synchronizedList(new ArrayList<>());
    private final List<Trap> traps            = Collections.synchronizedList(new ArrayList<>());

    private int roundTicksRemaining = 2400;
    private String lastWinner = null;
    private final List<String> deathMessages = Collections.synchronizedList(new ArrayList<>());
    private long tick = 0;

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public List<Player> alivePlayers() {
        return players.values().stream().filter(Player::isAlive).toList();
    }

    public void resetRound() {
        structures.clear();
        traps.clear();
        deathMessages.clear();
        roundTicksRemaining = 2400;
        phase = Phase.ACTIVE;
        lastWinner = null;
        tick = 0;
    }
}