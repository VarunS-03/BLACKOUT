package blackout.structures;
import java.util.*;
public class LRUCache<K,V> {
 private final int capacity;private final LinkedHashMap<K,V> map;
 public LRUCache(int capacity){if(capacity<=0)throw new IllegalArgumentException("capacity must be positive");this.capacity=capacity;map=new LinkedHashMap<>(16,0.75f,true);}
 public V get(K key){return map.get(key);}
 public void put(K key,V value){map.put(key,value);if(map.size()>capacity){K eldest=map.keySet().iterator().next();map.remove(eldest);}}
 public int size(){return map.size();}public boolean containsKey(K key){return map.containsKey(key);}
}
