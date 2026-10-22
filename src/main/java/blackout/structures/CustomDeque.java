package blackout.structures;

import java.util.NoSuchElementException;

public class CustomDeque<T> {
    private static final class Node<T> {
        private final T value;
        private Node<T> previous;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }

    private Node<T> first;
    private Node<T> last;
    private int size;

    public void addFirst(T value) {
        Node<T> newNode = new Node<>(value);
        if (first == null) {
            first = newNode;
            last = newNode;
        } else {
            newNode.next = first;
            first.previous = newNode;
            first = newNode;
        }
        size++;
    }

    public void addLast(T value) {
        Node<T> newNode = new Node<>(value);
        if (last == null) {
            first = newNode;
            last = newNode;
        } else {
            last.next = newNode;
            newNode.previous = last;
            last = newNode;
        }
        size++;
    }

    public T removeFirst() {
        if (first == null) {
            throw new NoSuchElementException("Deque is empty");
        }
        T value = first.value;
        first = first.next;
        if (first == null) {
            last = null;
        } else {
            first.previous = null;
        }
        size--;
        return value;
    }

    public T removeLast() {
        if (last == null) {
            throw new NoSuchElementException("Deque is empty");
        }
        T value = last.value;
        last = last.previous;
        if (last == null) {
            first = null;
        } else {
            last.next = null;
        }
        size--;
        return value;
    }

    public T peekFirst() {
        return first == null ? null : first.value;
    }

    public T peekLast() {
        return last == null ? null : last.value;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
