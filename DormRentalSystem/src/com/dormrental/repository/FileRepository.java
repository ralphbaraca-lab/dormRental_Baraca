package com.dormrental.repository;

import com.dormrental.exception.DormRentalException;
import com.dormrental.model.Identifiable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Keeps all entities of one type in memory and writes them to a single file
 * (Java serialization) after every change, so data survives restarts.
 */
public class FileRepository<T extends Identifiable & Serializable> implements Repository<T> {
    private final Path file;
    private final String idPrefix;
    private final Map<String, T> store = new LinkedHashMap<>();

    public FileRepository(String dataDir, String name, String idPrefix) {
        this.file = Paths.get(dataDir, name + ".dat");
        this.idPrefix = idPrefix;
        load();
    }

    @Override
    public T save(T entity) {
        store.put(entity.getId(), entity);
        persist();
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
        if (store.remove(id) != null) {
            persist();
        }
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
        return String.format("%s-%04d", idPrefix, max + 1);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            store.putAll((Map<String, T>) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new DormRentalException("Could not read " + file
                    + " (" + e.getMessage() + "). If the file is damaged, delete it to start fresh.", e);
        }
    }

    private void persist() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(new LinkedHashMap<>(store));
            }
        } catch (IOException e) {
            throw new DormRentalException("Could not save " + file + ": " + e.getMessage(), e);
        }
    }
}
