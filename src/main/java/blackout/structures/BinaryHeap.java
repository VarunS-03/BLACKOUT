package blackout.structures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class BinaryHeap<T> {
    private final ArrayList<T> values = new ArrayList<>();
    private final Comparator<? super T> comparator;

    public BinaryHeap(Comparator<? super T> comparator) {
        this.comparator = Objects.requireNonNull(comparator);
    }

    public void add(T value) {
        values.add(value);
        moveUp(values.size() - 1);
    }

    public T peek() {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }
        return values.get(0);
    }

    public T remove() {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }

        T firstValue = values.get(0);
        T lastValue = values.remove(values.size() - 1);
        if (!values.isEmpty()) {
            values.set(0, lastValue);
            moveDown(0);
        }
        return firstValue;
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    private void moveUp(int index) {
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            if (comparator.compare(values.get(index), values.get(parentIndex)) >= 0) {
                return;
            }
            swap(index, parentIndex);
            index = parentIndex;
        }
    }

    private void moveDown(int index) {
        while (true) {
            int leftIndex = 2 * index + 1;
            int rightIndex = leftIndex + 1;
            int firstIndex = index;

            if (leftIndex < values.size()
                    && comparator.compare(values.get(leftIndex), values.get(firstIndex)) < 0) {
                firstIndex = leftIndex;
            }
            if (rightIndex < values.size()
                    && comparator.compare(values.get(rightIndex), values.get(firstIndex)) < 0) {
                firstIndex = rightIndex;
            }
            if (firstIndex == index) {
                return;
            }

            swap(index, firstIndex);
            index = firstIndex;
        }
    }

    private void swap(int firstIndex, int secondIndex) {
        T firstValue = values.get(firstIndex);
        values.set(firstIndex, values.get(secondIndex));
        values.set(secondIndex, firstValue);
    }
}
