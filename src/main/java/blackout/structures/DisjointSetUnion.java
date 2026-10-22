package blackout.structures;

public class DisjointSetUnion {
    private final int[] parent;
    private final int[] rank;

    public DisjointSetUnion(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative");
        }
        parent = new int[size];
        rank = new int[size];
        for (int index = 0; index < size; index++) {
            parent[index] = index;
        }
    }

    public int find(int value) {
        checkIndex(value);
        if (parent[value] != value) {
            parent[value] = find(parent[value]);
        }
        return parent[value];
    }

    public boolean union(int first, int second) {
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }

        if (rank[firstRoot] < rank[secondRoot]) {
            parent[firstRoot] = secondRoot;
        } else if (rank[firstRoot] > rank[secondRoot]) {
            parent[secondRoot] = firstRoot;
        } else {
            parent[secondRoot] = firstRoot;
            rank[firstRoot]++;
        }
        return true;
    }

    public boolean connected(int first, int second) {
        return find(first) == find(second);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= parent.length) {
            throw new IndexOutOfBoundsException("node " + index);
        }
    }
}
