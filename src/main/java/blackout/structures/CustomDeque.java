package blackout.structures;

import java.util.*;

public class CustomDeque<T> {
    private final LinkedList<T> list = new LinkedList<>();

    public void addFirst(T v) {
        list.addFirst(v);
    }

    public void addLast(T v) {
        list.addLast(v);
    }

    public T removeFirst() {
        return list.removeFirst();
    }

    public T removeLast() {
        return list.removeLast();
    }

    public T peekFirst() {
        return list.peekFirst();
    }

    public T peekLast() {
        return list.peekLast();
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
}
