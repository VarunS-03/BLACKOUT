package blackout.structures;

import java.util.NoSuchElementException;

public class CircularQueue<T> {
    private final Object[] values;
    private int head;
    private int tail;
    private int size;

    public CircularQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        values = new Object[capacity];
    }

    public boolean offer(T value) {
        if (size == values.length) {
            return false;
        }
        values[tail] = value;
        tail = (tail + 1) % values.length;
        size++;
        return true;
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        if (size == 0) {
            throw new NoSuchElementException("Queue is empty");
        }

        T value = (T) values[head];
        values[head] = null;
        head = (head + 1) % values.length;
        size--;
        return value;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return values.length;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
