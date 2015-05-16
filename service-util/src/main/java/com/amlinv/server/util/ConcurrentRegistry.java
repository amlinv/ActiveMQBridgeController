package com.amlinv.server.util;

import com.amlinv.server.util.listener.NotificationExecutor;
import com.amlinv.server.util.listener.SimpleSynchronousNotificationExecutor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory registry backed by a concurrent map which supports listeners and a configurable notification strategy
 * defined by the configured notification executor.
 *
 * Created by art on 5/5/15.
 */
public class ConcurrentRegistry<K, V> {
    private final ConcurrentHashMap<K, V>   store = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<RegistryListener<K, V>> listeners = new ConcurrentLinkedDeque<>();

    private final NotificationExecutor<K, V> notificationExecutor;

    /**
     * Create a new registry using the default notification executor class, SimpleSynchronousNotificationExecutor.
     * See also the warnings in the SimpleSynchronousNotificationExecutor.
     *
     * @see com.amlinv.server.util.listener.SimpleSynchronousNotificationExecutor
     */
    public ConcurrentRegistry() {
        this(new SimpleSynchronousNotificationExecutor<K, V>());
    }

    /**
     * Create a new registry with the notification executor given.
     *
     * @param notificationExecutor executor of notifications.
     */
    public ConcurrentRegistry(NotificationExecutor<K, V> notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    /**
     * Setter for listeners for use with Spring; adds the listeners to the existing list, if any.
     *
     * @param newListeners listeners to add.
     */
    public void setListeners (List<RegistryListener<K, V>> newListeners) {
        this.listeners.addAll(newListeners);
    }

    /**
     * Add the given listener to the list of listeners for this registry.  See also the notes on removeListener().
     * Note that adding the same listener more than once will result in that listener receiving duplicate notifications.
     *
     * @param addListener new listener to add to the registry.
     */
    public void addListener (RegistryListener<K, V> addListener) {
        this.listeners.add(addListener);
    }

    /**
     * Remove the given listener from the list of listeners; note this removal is a slow operation and using it
     * frequently is an anti-pattern.  Adding and removing listeners are expected to be rare operations, most likely
     * only happening during system startup and shutdown.
     *
     * @param removeListener remove the listener from the registry.
     */
    public void removeListener (RegistryListener<K, V> removeListener) {
        this.listeners.remove(removeListener);
    }

    /**
     * Return the entry in the registry identified by the given key.
     *
     * @param key identifier of the entry in the registry.
     * @return entry in the registry, if known; null otherwise.
     */
    public V get(K key) {
        return  this.store.get(key);
    }

    /**
     * Determine if the given key is in the registry.
     *
     * @param key key to verify in the registry.
     * @return true => if the key exists in the registry; false => otherwise.
     */
    public boolean containsKey (K key) {
        return  this.store.containsKey(key);
    }

    /**
     * Put the given entry into the registry under the specified key.
     *
     * @param putKey
     * @param putValue
     * @return
     */
    public V put (K putKey, V putValue) {
        V oldValue = this.store.put(putKey, putValue);

        if ( oldValue == null ) {
            this.notificationExecutor.firePutNotification(this.listeners.iterator(), putKey, putValue);
        } else {
            this.notificationExecutor.fireReplaceNotification(this.listeners.iterator(), putKey, oldValue, putValue);
        }

        return  oldValue;
    }

    /**
     * Add the given entry into the registry under the specified key, but only if the key is not already registered.
     *
     * @param putKey key identifying the entry in the registry to add, if it does not already exist.
     * @param putValue value to add to the registry.
     * @return existing value in the registry if already defined; null if the new value is added to the registry.
     */
    public V putIfAbsent (K putKey, V putValue) {
        V existingValue = this.store.putIfAbsent(putKey, putValue);

        if ( existingValue == null ) {
            this.notificationExecutor.firePutNotification(this.listeners.iterator(), putKey, putValue);
        }

        return  existingValue;
    }

    /**
     * Remove the given entry from the registry under the specified key.
     *
     * @param removeKey key of the entry to be removed.
     * @return value of the removed entry; null if no value was removed.
     */
    public V remove (K removeKey) {
        V removedValue = this.store.remove(removeKey);

        if ( removedValue != null ) {
            this.notificationExecutor.fireRemoveNotification(this.listeners.iterator(), removeKey, removedValue);
        }

        return  removedValue;
    }

    /**
     * Remove the given entry from the registry under the specified key, only if the value matches the one given.
     *
     * @param removeKey key of the entry to be removed.
     * @param removeValue value of the entry to be removed.
     * @return true => if the value was removed; false => otherwise.
     */
    public boolean remove (K removeKey, V removeValue) {
        boolean removedInd = this.store.remove(removeKey, removeValue);

        if ( removedInd ) {
            this.notificationExecutor.fireRemoveNotification(this.listeners.iterator(), removeKey, removeValue);
        }

        return  removedInd;
    }

    /**
     * Return a read-only view of the keys in the registry.
     *
     * @return unmodifiable collection of the keys in the registry; note the set is backed by the registry so changes
     * to the registry will be reflected in the returned set.
     */
    public Set<K> keys () {
        return Collections.unmodifiableSet(this.store.keySet());
    }

    /**
     * Return a read-only view of the values in the registry.
     *
     * @return unmodifiable collection of the values in the registry; note the collection is backed by the registry
     * so changes to the registry will be reflected in the returned collection.
     */
    public Collection<V> values () {
        return Collections.unmodifiableCollection(this.store.values());
    }
}
