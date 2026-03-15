package com.chaosarena.room;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManager {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Room createRoom() {
        String code = generateCode();
        Room room = new Room(code);
        rooms.put(code, room);
        return room;
    }

    public Optional<Room> findRoom(String code) {
        return Optional.ofNullable(rooms.get(code.toUpperCase()));
    }

    public void removeRoomIfEmpty(String code) {
        rooms.computeIfPresent(code, (k, room) -> room.isEmpty() ? null : room);
    }

    public Collection<Room> getAllRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    public int activeRoomCount() {
        return rooms.size();
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        do {
            StringBuilder sb = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (rooms.containsKey(code));
        return code;
    }
}