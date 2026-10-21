package blackout.structures;

import java.util.NoSuchElementException;

public class CircularQueue<T> {
    private final Object[] data;
    private int head = 0, tail = 0, size = 0;

    public CircularQueue(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity must be positive");
        data = new Object[capacity];
    }

    public boolean offer(T v) {
        if (size == data.length)
            return false;
        data[tail] = v;
        tail = (tail + 1) % data.length;
        size++;
        return true;
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        if (size == 0)
            throw new NoSuchElementException("Queue empty");
        T v = (T) data[head];
        data[head] = null;
        head = (head + 1) % data.length;
        size--;
        return v;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
