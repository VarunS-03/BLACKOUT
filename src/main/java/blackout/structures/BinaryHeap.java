package blackout.structures;

import java.util.*;

public class BinaryHeap<T> {
    private final ArrayList<T> a = new ArrayList<>();
    private final Comparator<? super T> cmp;

    public BinaryHeap(Comparator<? super T> cmp) {
        this.cmp = Objects.requireNonNull(cmp);
    }

    public void add(T v) {
        a.add(v);
        up(a.size() - 1);
    }

    public T peek() {
        if (a.isEmpty())
            throw new NoSuchElementException("Heap empty");
        return a.get(0);
    }

    public T remove() {
        if (a.isEmpty())
            throw new NoSuchElementException("Heap empty");
        T out = a.get(0), last = a.remove(a.size() - 1);
        if (!a.isEmpty()) {
            a.set(0, last);
            down(0);
        }
        return out;
    }

    public int size() {
        return a.size();
    }

    public boolean isEmpty() {
        return a.isEmpty();
    }

    private void up(int i) {
        while (i > 0) {
            int p = (i - 1) / 2;
            if (cmp.compare(a.get(i), a.get(p)) >= 0)
                break;
            swap(i, p);
            i = p;
        }
    }

    private void down(int i) {
        while (true) {
            int l = 2 * i + 1, r = l + 1, s = i;
            if (l < a.size() && cmp.compare(a.get(l), a.get(s)) < 0)
                s = l;
            if (r < a.size() && cmp.compare(a.get(r), a.get(s)) < 0)
                s = r;
            if (s == i)
                return;
            swap(i, s);
            i = s;
        }
    }

    private void swap(int i, int j) {
        T t = a.get(i);
        a.set(i, a.get(j));
        a.set(j, t);
    }
}
