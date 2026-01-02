package de.uni_jena.fpp.chatroom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileUserRepository implements UserRepository {

    private final Path file;
    private final int defaultIterations;

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public FileUserRepository(String filePath, int defaultIterations) {
        this.file = Path.of(filePath);
        this.defaultIterations = Math.max(10_000, defaultIterations);
        loadAll();
    }

    @Override
    public synchronized boolean createUser(String username, char[] password) {
        if (!isValidName(username) || password == null) return false;
        if (users.containsKey(username)) return false;

        byte[] salt = PasswordUtil.newSalt();
        byte[] hash = PasswordUtil.pbkdf2(password, salt, defaultIterations);

        User u = new User(username, defaultIterations, salt, hash, false);
        users.put(username, u);

        return saveAll();
    }

    @Override
    public User verifyLogin(String username, char[] password) {
        if (username == null || password == null) return null;
        User u = users.get(username);
        if (u == null) return null;
        return PasswordUtil.matches(password, u) ? u : null;
    }

    @Override
    public List<User> listUsers() {
        List<User> list = new ArrayList<>(users.values());
        list.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    @Override
    public synchronized boolean setBanned(String username, boolean banned) {
        if (username == null) return false;
        User old = users.get(username);
        if (old == null) return false;

        users.put(username, old.withBanned(banned));
        return saveAll();
    }

    private synchronized void loadAll() {
        users.clear();

        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            if (!Files.exists(file)) return;

            try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    User u = parseLine(line);
                    if (u != null) users.put(u.getUsername(), u);
                }
            }
        } catch (IOException e) {
            System.err.println("[USER-REPO] Laden fehlgeschlagen: " + e.getMessage());
        }
    }

    private synchronized boolean saveAll() {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());

            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");

            try (BufferedWriter bw = Files.newBufferedWriter(
                    tmp,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                bw.write("# username;iterations;saltBase64;hashBase64;banned");
                bw.newLine();

                for (User u : listUsers()) {
                    bw.write(toLine(u));
                    bw.newLine();
                }
            }

            Files.move(tmp, file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            return true;

        } catch (IOException e) {
            System.err.println("[USER-REPO] Speichern fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    private User parseLine(String line) {
        String[] p = line.split(";", 5);
        if (p.length < 5) return null;

        try {
            String username = p[0];
            int it = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] hash = Base64.getDecoder().decode(p[3]);
            boolean banned = Boolean.parseBoolean(p[4]);

            if (!isValidName(username)) return null;
            return new User(username, it, salt, hash, banned);

        } catch (Exception e) {
            return null;
        }
    }

    private String toLine(User u) {
        String saltB64 = Base64.getEncoder().encodeToString(u.getSalt());
        String hashB64 = Base64.getEncoder().encodeToString(u.getPasswordHash());
        return u.getUsername() + ";" + u.getIterations() + ";" + saltB64 + ";" + hashB64 + ";" + u.isBanned();
    }

    private boolean isValidName(String s) {
        if (s == null) return false;
        if (s.isBlank()) return false;
        return !s.contains(" ") && !s.contains("|") && !s.contains(";");
    }
}
