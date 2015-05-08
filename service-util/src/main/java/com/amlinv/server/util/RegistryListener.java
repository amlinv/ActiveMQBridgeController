package com.amlinv.server.util;

/**
 * Created by art on 5/5/15.
 */
public interface RegistryListener<K, V> {
    /**
     * Notification immediately after an element is added to the registry.  Note this method is called synchronously
     * within the add operation and is therefore expected to be performed quickly.
     *
     * @param putKey key of the entry that was added to the registry.
     * @param putValue value of the entry that was added to the registry.
     */
    void    onPutEntry(K putKey, V putValue);

    /**
     * Notification immediately after an element is removed from the registry.  Note this method is called synchronously
     * within the remove operation and is therefore expected to be performed quickly.
     *
     * @param removeKey key of the entry that was removed.
     * @param removeValue value of the entry that was removed.
     */
    void    onRemoveEntry(K removeKey, V removeValue);

    /**
     * Notification immediately before an element is stored in the registry.
     * @param replaceKey
     * @param oldValue
     * @param newValue
     */
    void    onReplaceEntry(K replaceKey, V oldValue, V newValue);
}
