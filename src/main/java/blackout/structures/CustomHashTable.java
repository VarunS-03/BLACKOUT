package blackout.structures;
import java.util.Objects;
public class CustomHashTable<K,V> {
    private static final class Entry<K,V>{K k;V v;Entry<K,V> next;Entry(K k,V v){this.k=k;this.v=v;}}
    private Entry<K,V>[] buckets; private int size;
    @SuppressWarnings("unchecked") public CustomHashTable(){buckets=(Entry<K,V>[])new Entry[16];}
    private int index(Object k){return (k.hashCode()&0x7fffffff)%buckets.length;}
    public void put(K k,V v){Objects.requireNonNull(k);int i=index(k);for(Entry<K,V>e=buckets[i];e!=null;e=e.next)if(e.k.equals(k)){e.v=v;return;}Entry<K,V>n=new Entry<>(k,v);n.next=buckets[i];buckets[i]=n;size++;if(size>buckets.length*2)resize();}
    public V get(K k){if(k==null)return null;for(Entry<K,V>e=buckets[index(k)];e!=null;e=e.next)if(e.k.equals(k))return e.v;return null;}
    public boolean containsKey(K k){if(k==null)return false;for(Entry<K,V>e=buckets[index(k)];e!=null;e=e.next)if(e.k.equals(k))return true;return false;}
    @SuppressWarnings("unchecked") private void resize(){Entry<K,V>[] old=buckets;buckets=(Entry<K,V>[])new Entry[old.length*2];size=0;for(Entry<K,V>e:old)for(;e!=null;e=e.next)put(e.k,e.v);}
    public int size(){return size;}
}