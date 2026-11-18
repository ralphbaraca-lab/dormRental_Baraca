package com.dormrental.repository;

import com.dormrental.model.Identifiable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Storage abstraction. Services depend on this interface, not on how data is stored.
 *
 * <p>Methods that change data ({@link #save}, {@link #deleteById}) either complete and are persisted,
 * or throw and leave the stored data as it was.
 */
public interface Repository<T extends Identifiable> {

    /** Inserts or replaces the entity (matched by id) and persists the change. Returns the saved entity. */
    T save(T entity);

    /** The entity with this id, or empty if there is none (or the id is null). */
    Optional<T> findById(String id);

    /** Every entity, in the order they were first saved. The list is new, so the caller may sort or change it. */
    List<T> findAll();

    /** Every entity the filter accepts, in the order they were first saved. The list is new, so the caller may change it. */
    List<T> findAll(Predicate<T> filter);

    boolean existsById(String id);

    /** Removes the entity and persists the change. Does nothing if there is no such entity. */
    void deleteById(String id);

    int count();

    /**
     * Next free id such as "TNT-0007". The id is not reserved: calling this twice without saving in
     * between returns the same id both times.
     */
    String nextId();

    /** The first entity the filter accepts, if any. */
    default Optional<T> findFirst(Predicate<T> filter) {
        return findAll(filter).stream().findFirst();
    }

    /** True if at least one entity is accepted by the filter. */
    default boolean exists(Predicate<T> filter) {
        return !findAll(filter).isEmpty();
    }
}