package com.amlinv.server.util.listener;

import com.amlinv.server.util.RegistryListener;

import java.util.Iterator;

/**
 * Created by art on 5/5/15.
 */
public interface NotificationExecutor<K, V> {
    /**
     * Fire notification of a new entry added to the registry.
     *
     * @param putKey key identifying the entry in the registry.
     * @param putValue value of the entry in the registry.
     */
    void firePutNotification(Iterator<RegistryListener<K, V>> listeners, K putKey, V putValue);

    /**
     * Fire notification of an entry that was just removed from the registry.
     *
     * @param removeKey key identifying the entry removed from the registry.
     * @param removeValue value of the entry in the registry.
     */
    void fireRemoveNotification (Iterator<RegistryListener<K, V>> listeners, K removeKey, V removeValue);

    /**
     * Fire notification of an entry for which the value was just replaced in the registry.
     * @param replaceKey key identifying the entry in the registry for which the value was replaced.
     * @param oldValue old value of the entry in the registry.
     * @param newValue new value of the entry in the registry.
     */
    void fireReplaceNotification (Iterator<RegistryListener<K, V>> listeners, K replaceKey, V oldValue, V newValue);
}
