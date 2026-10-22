package blackout.structures;

import java.util.ArrayList;
import java.util.NoSuchElementException;

public class CustomStack<T> {
    private final ArrayList<T> values = new ArrayList<>();

    public void push(T value) {
        values.add(value);
    }

    public T pop() {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }
        return values.remove(values.size() - 1);
    }

    public T peek() {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }
        return values.get(values.size() - 1);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
