package blackout.structures;

import blackout.model.RecoveryTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class StructureBehaviorTest {
    @Test
    void linkedListQueueAndStackKeepTheirOrderAndCanBeReused() {
        CustomLinkedList<Integer> list = new CustomLinkedList<>();
        assertFalse(list.removeFirstMatch(1));
        list.add(1);
        list.add(2);
        assertTrue(list.removeFirstMatch(1));
        assertEquals(List.of(2), collect(list));
        assertEquals(1, list.size());
        assertTrue(list.removeFirstMatch(2));
        assertTrue(list.isEmpty());
        list.add(3);
        assertEquals(List.of(3), collect(list));

        CustomQueue<Integer> queue = new CustomQueue<>();
        queue.enqueue(1);
        queue.enqueue(2);
        assertEquals(1, queue.dequeue());
        assertEquals(2, queue.dequeue());
        queue.enqueue(3);
        assertEquals(3, queue.dequeue());
        assertTrue(queue.isEmpty());
        assertThrows(NoSuchElementException.class, queue::dequeue);

        CustomStack<Integer> stack = new CustomStack<>();
        stack.push(1);
        stack.push(2);
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertThrows(NoSuchElementException.class, stack::pop);
    }

    @Test
    void hashTableResolvesCollisionsUpdatesAndResizes() {
        CustomHashTable<CollisionKey, String> table = new CustomHashTable<>();
        for (int i = 0; i < 40; i++) table.put(new CollisionKey(i), "v" + i);
        table.put(new CollisionKey(7), "updated");
        assertEquals(40, table.size());
        assertEquals("updated", table.get(new CollisionKey(7)));
        assertEquals("v39", table.get(new CollisionKey(39)));
        assertFalse(table.containsKey(new CollisionKey(100)));
    }

    @Test
    void graphAndDisjointSetReportExpectedConnectivity() {
        Graph graph = new Graph();
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(2, 3);
        assertEquals(List.of(1, 2, 3), graph.bfs(1));
        assertEquals(List.of(1, 2, 3), graph.dfs(1));
        assertEquals(List.of(), graph.bfs(99));
        assertTrue(graph.removeEdge(2, 3));
        assertEquals(List.of(1, 2), graph.bfs(1));

        DisjointSetUnion sets = new DisjointSetUnion(4);
        assertTrue(sets.union(0, 1));
        assertTrue(sets.union(2, 3));
        assertTrue(sets.union(1, 2));
        assertTrue(sets.connected(0, 3));
        assertFalse(sets.union(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> sets.connected(0, 4));
    }

    @Test
    void treesHeapAndPriorityQueueMaintainOrdering() {
        BinarySearchTree<Integer> bst = new BinarySearchTree<>();
        for (int value : new int[]{4, 2, 6, 2, 1, 5}) bst.insert(value);
        assertTrue(bst.contains(5));
        assertFalse(bst.contains(3));
        assertEquals(List.of(1, 2, 4, 5, 6), bst.inorder());

        BinaryTree<Integer> tree = new BinaryTree<>();
        assertEquals(List.of(), tree.inorder());
        tree.setRoot(new BinaryTree.Node<>(2));
        tree.getRoot().left = new BinaryTree.Node<>(1);
        tree.getRoot().right = new BinaryTree.Node<>(3);
        assertEquals(List.of(2, 1, 3), tree.preorder());
        assertEquals(List.of(1, 2, 3), tree.inorder());
        assertEquals(List.of(1, 3, 2), tree.postorder());

        BinaryHeap<Integer> heap = new BinaryHeap<>(Comparator.naturalOrder());
        for (int value : new int[]{5, 1, 4, 1}) heap.add(value);
        List<Integer> sorted = new ArrayList<>();
        while (!heap.isEmpty()) sorted.add(heap.remove());
        assertEquals(List.of(1, 1, 4, 5), sorted);
        assertThrows(NoSuchElementException.class, heap::remove);

        CustomPriorityQueue tasks = new CustomPriorityQueue();
        tasks.add(new RecoveryTask(2, 2, 1, 20, "later"));
        tasks.add(new RecoveryTask(3, 3, 1, 10, "sooner"));
        tasks.add(new RecoveryTask(1, 1, 2, 1, "less severe"));
        assertEquals(3, tasks.poll().getId());
        assertEquals(2, tasks.poll().getId());
        assertEquals(1, tasks.poll().getId());
    }

    @Test
    void circularQueueDequeAndSegmentTreeHandleBoundaries() {
        CircularQueue<Integer> queue = new CircularQueue<>(2);
        assertTrue(queue.offer(1));
        assertTrue(queue.offer(2));
        assertFalse(queue.offer(3));
        assertEquals(1, queue.poll());
        assertTrue(queue.offer(3));
        assertEquals(2, queue.poll());
        assertEquals(3, queue.poll());
        assertTrue(queue.isEmpty());

        CustomDeque<Integer> deque = new CustomDeque<>();
        assertNull(deque.peekFirst());
        assertNull(deque.peekLast());
        assertThrows(NoSuchElementException.class, deque::removeFirst);
        deque.addFirst(2);
        deque.addFirst(1);
        deque.addLast(3);
        assertEquals(3, deque.size());
        assertEquals(1, deque.peekFirst());
        assertEquals(3, deque.peekLast());
        assertEquals(1, deque.removeFirst());
        assertEquals(3, deque.removeLast());
        assertEquals(2, deque.removeFirst());
        assertTrue(deque.isEmpty());

        SegmentTree sums = new SegmentTree(new int[]{2, -1, 4, 3});
        assertEquals(8, sums.rangeSum(0, 3));
        assertEquals(3, sums.rangeSum(1, 2));
        sums.update(1, 5);
        assertEquals(11, sums.rangeSum(0, 2));
        assertThrows(IllegalArgumentException.class, () -> sums.rangeSum(0, 4));
        assertThrows(IllegalArgumentException.class, () -> new SegmentTree(new int[0]).rangeSum(0, 0));
    }

    @Test
    void trieCacheAndMerkleTreeVerifyObservableBehavior() {
        Trie trie = new Trie();
        trie.insert("Data");
        trie.insert("database");
        trie.insert("Data");
        assertTrue(trie.contains("DATA"));
        assertFalse(trie.contains("dat"));
        assertEquals(List.of("data", "database"), trie.wordsWithPrefix("DAT"));

        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals("one", cache.get(1));
        cache.put(3, "three");
        assertTrue(cache.containsKey(1));
        assertFalse(cache.containsKey(2));

        List<String> records = List.of("alpha", "beta", "gamma");
        MerkleTree tree = new MerkleTree(records);
        assertEquals(tree.getRoot(), new MerkleTree(records).getRoot());
        List<String> proof = tree.proof(1);
        assertTrue(tree.verify("beta", 1, proof, tree.getRoot()));
        assertTrue(tree.verify("gamma", 2, tree.proof(2), tree.getRoot()));
        assertFalse(tree.verify("BETA", 1, proof, tree.getRoot()));
        assertFalse(tree.verify("beta", 0, proof, tree.getRoot()));
        assertFalse(tree.verify("beta", 1, List.of("R:invalid"), tree.getRoot()));
        assertFalse(new MerkleTree(List.of()).verify("", 0, List.of(), tree.getRoot()));
    }

    private static <T> List<T> collect(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        for (T value : values) result.add(value);
        return result;
    }

    private static final class CollisionKey {
        private final int value;
        private CollisionKey(int value) { this.value = value; }
        @Override public int hashCode() { return Integer.MIN_VALUE; }
        @Override public boolean equals(Object other) {
            return other instanceof CollisionKey key && value == key.value;
        }
    }
}
