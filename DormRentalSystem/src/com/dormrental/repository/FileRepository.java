package com.dormrental.repository;

import com.dormrental.exception.DormRentalException;
import com.dormrental.model.Identifiable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Keeps all entities of one type in memory and writes them to a single file
 * (Java serialization) after every change, so data survives restarts.
 *
 * <p>A change is written to disk first and only then applied in memory, so if saving fails
 * (disk full, file locked) the in-memory data still matches the file. The file itself is replaced
 * in one step, so a crash in the middle of a save can't leave a half-written file.
 */
public class FileRepository<T extends Identifiable & Serializable> implements Repository<T> {
    private final Path file;
    private final String idPrefix;
    private final Map<String, T> store = new LinkedHashMap<>();

    public FileRepository(String dataDir, String name, String idPrefix) {
        this.file = Paths.get(Objects.requireNonNull(dataDir, "dataDir"), Objects.requireNonNull(name, "name") + ".dat");
        this.idPrefix = Objects.requireNonNull(idPrefix, "idPrefix");
        load();
    }

    @Override
    public T save(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        String id = Objects.requireNonNull(entity.getId(), "entity id must not be null");

        Map<String, T> updated = new LinkedHashMap<>(store);
        updated.put(id, entity);
        persist(updated);   // if this throws, the store is untouched
        store.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(String id) {
        return Optional.ofNullable(id == null ? null : store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<T> findAll(Predicate<T> filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        List<T> result = new ArrayList<>();
        for (T entity : store.values()) {
            if (filter.test(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public boolean existsById(String id) {
        return id != null && store.containsKey(id);
    }

    @Override
    public void deleteById(String id) {
        if (!existsById(id)) {
            return;
        }
        Map<String, T> updated = new LinkedHashMap<>(store);
        updated.remove(id);
        persist(updated);   // if this throws, the entity is still in the store
        store.remove(id);
    }

    @Override
    public int count() {
        return store.size();
    }

    @Override
    public String nextId() {
        int max = 0;
        String prefix = idPrefix + "-";
        for (String id : store.keySet()) {
            if (id.startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // not one of our generated ids; skip it
                }
            }
        }
        return String.format(Locale.ROOT, "%s-%04d", idPrefix, max + 1);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            store.putAll((Map<String, T>) in.readObject());
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new DormRentalException("Could not read " + file
                    + " (" + e.getMessage() + "). If the file is damaged, delete it to start fresh.", e);
        }
    }

    /**
     * Writes the whole snapshot to a temporary file next to the real one, then swaps it in.
     * Writing straight over the real file would lose everything if the program stopped part-way through.
     * The file format is the same as before, so existing .dat files still load.
     */
    private void persist(Map<String, T> snapshot) {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (FileOutputStream raw = new FileOutputStream(temp.toFile());
                 ObjectOutputStream out = new ObjectOutputStream(raw)) {
                out.writeObject(snapshot);
                out.flush();
                raw.getFD().sync(); // make sure it is on the disk, not just in the operating system's cache
            }
            moveIntoPlace(temp);
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new DormRentalException("Could not save " + file + ": " + e.getMessage(), e);
        }
    }

    private void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Some file systems can't swap in one step; this is the next best thing.
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // nothing more can be done; the original error is the one worth reporting
        }
    }
}