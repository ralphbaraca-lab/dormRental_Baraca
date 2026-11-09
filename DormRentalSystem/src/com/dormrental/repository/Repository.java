package com.dormrental.repository;

import com.dormrental.model.Identifiable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Storage abstraction. Services depend on this interface, not on how data is stored. */
public interface Repository<T extends Identifiable> {

    /** Inserts or replaces the entity (matched by id) and persists the change. */
    T save(T entity);

    Optional<T> findById(String id);

    List<T> findAll();

    List<T> findAll(Predicate<T> filter);

    boolean existsById(String id);

    void deleteById(String id);

    int count();

    /** Next free id such as "TNT-0007". */
    String nextId();
}
