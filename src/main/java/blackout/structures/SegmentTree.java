package blackout.structures;

public class SegmentTree {
    private final int valueCount;
    private final long[] tree;

    public SegmentTree(int[] values) {
        valueCount = values.length;
        tree = new long[Math.max(1, 4 * valueCount)];
        if (valueCount > 0) {
            build(1, 0, valueCount - 1, values);
        }
    }

    public long rangeSum(int firstIndex, int lastIndex) {
        if (firstIndex < 0 || lastIndex >= valueCount || firstIndex > lastIndex) {
            throw new IllegalArgumentException("invalid range");
        }
        return rangeSum(1, 0, valueCount - 1, firstIndex, lastIndex);
    }

    public void update(int index, int value) {
        if (index < 0 || index >= valueCount) {
            throw new IndexOutOfBoundsException("index " + index);
        }
        update(1, 0, valueCount - 1, index, value);
    }

    private void build(int node, int firstIndex, int lastIndex, int[] values) {
        if (firstIndex == lastIndex) {
            tree[node] = values[firstIndex];
            return;
        }

        int middle = (firstIndex + lastIndex) / 2;
        int leftChild = node * 2;
        int rightChild = leftChild + 1;
        build(leftChild, firstIndex, middle, values);
        build(rightChild, middle + 1, lastIndex, values);
        tree[node] = tree[leftChild] + tree[rightChild];
    }

    private long rangeSum(
            int node, int firstIndex, int lastIndex, int queryFirst, int queryLast) {
        if (queryFirst <= firstIndex && lastIndex <= queryLast) {
            return tree[node];
        }

        int middle = (firstIndex + lastIndex) / 2;
        long sum = 0;
        if (queryFirst <= middle) {
            sum += rangeSum(node * 2, firstIndex, middle, queryFirst, queryLast);
        }
        if (queryLast > middle) {
            sum += rangeSum(node * 2 + 1, middle + 1, lastIndex, queryFirst, queryLast);
        }
        return sum;
    }

    private void update(int node, int firstIndex, int lastIndex, int targetIndex, int value) {
        if (firstIndex == lastIndex) {
            tree[node] = value;
            return;
        }

        int middle = (firstIndex + lastIndex) / 2;
        if (targetIndex <= middle) {
            update(node * 2, firstIndex, middle, targetIndex, value);
        } else {
            update(node * 2 + 1, middle + 1, lastIndex, targetIndex, value);
        }
        tree[node] = tree[node * 2] + tree[node * 2 + 1];
    }
}
