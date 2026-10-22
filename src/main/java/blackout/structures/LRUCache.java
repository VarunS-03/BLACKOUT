package blackout.structures;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> values;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        values = new LinkedHashMap<>(16, 0.75f, true);
    }

    public V get(K key) {
        return values.get(key);
    }

    public void put(K key, V value) {
        values.put(key, value);
        if (values.size() > capacity) {
            Iterator<Map.Entry<K, V>> entries = values.entrySet().iterator();
            entries.next();
            entries.remove();
        }
    }

    public int size() {
        return values.size();
    }

    public boolean containsKey(K key) {
        return values.containsKey(key);
    }
}
