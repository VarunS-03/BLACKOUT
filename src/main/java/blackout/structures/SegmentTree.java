package blackout.structures;

public class SegmentTree {
    private final int n;
    private final long[] tree;

    public SegmentTree(int[] values) {
        n = values.length;
        tree = new long[Math.max(1, 4 * n)];
        if (n > 0)
            build(1, 0, n - 1, values);
    }

    private void build(int node, int l, int r, int[] v) {
        if (l == r) {
            tree[node] = v[l];
            return;
        }
        int m = (l + r) / 2;
        build(node * 2, l, m, v);
        build(node * 2 + 1, m + 1, r, v);
        tree[node] = tree[node * 2] + tree[node * 2 + 1];
    }

    public long rangeSum(int left, int right) {
        if (left < 0 || right >= n || left > right)
            throw new IllegalArgumentException("invalid range");
        return query(1, 0, n - 1, left, right);
    }

    private long query(int node, int l, int r, int ql, int qr) {
        if (ql <= l && r <= qr)
            return tree[node];
        int m = (l + r) / 2;
        long s = 0;
        if (ql <= m)
            s += query(node * 2, l, m, ql, qr);
        if (qr > m)
            s += query(node * 2 + 1, m + 1, r, ql, qr);
        return s;
    }

    public void update(int index, int value) {
        if (index < 0 || index >= n)
            throw new IndexOutOfBoundsException();
        update(1, 0, n - 1, index, value);
    }

    private void update(int node, int l, int r, int i, int v) {
        if (l == r) {
            tree[node] = v;
            return;
        }
        int m = (l + r) / 2;
        if (i <= m)
            update(node * 2, l, m, i, v);
        else
            update(node * 2 + 1, m + 1, r, i, v);
        tree[node] = tree[node * 2] + tree[node * 2 + 1];
    }
}
