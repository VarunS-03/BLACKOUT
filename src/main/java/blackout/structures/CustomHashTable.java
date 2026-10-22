package blackout.structures;

import java.util.Objects;

public class CustomHashTable<K, V> {
    private static final class Entry<K, V> {
        private final K key;
        private V value;
        private Entry<K, V> next;

        private Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Entry<K, V>[] buckets;
    private int size;

    @SuppressWarnings("unchecked")
    public CustomHashTable() {
        buckets = (Entry<K, V>[]) new Entry[16];
    }

    public void put(K key, V value) {
        Objects.requireNonNull(key, "key cannot be null");
        int bucketIndex = indexFor(key);

        for (Entry<K, V> entry = buckets[bucketIndex]; entry != null; entry = entry.next) {
            if (entry.key.equals(key)) {
                entry.value = value;
                return;
            }
        }

        Entry<K, V> newEntry = new Entry<>(key, value);
        newEntry.next = buckets[bucketIndex];
        buckets[bucketIndex] = newEntry;
        size++;
        if (size > buckets.length * 2) {
            resize();
        }
    }

    public V get(K key) {
        if (key == null) {
            return null;
        }
        for (Entry<K, V> entry = buckets[indexFor(key)]; entry != null; entry = entry.next) {
            if (entry.key.equals(key)) {
                return entry.value;
            }
        }
        return null;
    }

    public boolean containsKey(K key) {
        if (key == null) {
            return false;
        }
        for (Entry<K, V> entry = buckets[indexFor(key)]; entry != null; entry = entry.next) {
            if (entry.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    private int indexFor(Object key) {
        return (key.hashCode() & 0x7fffffff) % buckets.length;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] oldBuckets = buckets;
        buckets = (Entry<K, V>[]) new Entry[oldBuckets.length * 2];
        size = 0;

        for (Entry<K, V> bucket : oldBuckets) {
            for (Entry<K, V> entry = bucket; entry != null; entry = entry.next) {
                put(entry.key, entry.value);
            }
        }
    }
}
