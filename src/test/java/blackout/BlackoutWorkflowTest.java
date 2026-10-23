package blackout;

import blackout.model.RecoveryTask;
import blackout.model.Server;
import blackout.model.TelemetryEvent;
import blackout.service.DataCenterManager;
import blackout.service.FailureAnalysisService;
import blackout.service.RecoveryScheduler;
import blackout.service.TelemetryManager;
import blackout.structures.BinaryHeap;
import blackout.structures.BinarySearchTree;
import blackout.structures.BinaryTree;
import blackout.structures.CircularQueue;
import blackout.structures.CustomDeque;
import blackout.structures.CustomHashTable;
import blackout.structures.CustomLinkedList;
import blackout.structures.CustomQueue;
import blackout.structures.CustomStack;
import blackout.structures.DisjointSetUnion;
import blackout.structures.LRUCache;
import blackout.structures.MerkleTree;
import blackout.structures.SegmentTree;
import blackout.structures.Trie;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackoutWorkflowTest {
    @Test
    void serverInventoryUsesUniqueIdsAndProvidesUsefulSearchOrders() {
        DataCenterManager manager = new DataCenterManager();
        Server first = new Server(20, "Database");
        Server second = new Server(5, "Data API");

        assertTrue(manager.registerServer(first));
        assertTrue(manager.registerServer(second));
        assertFalse(manager.registerServer(new Server(20, "Duplicate")));
        assertEquals(first, manager.findServer(20));
        assertEquals(List.of(first, second), manager.registeredServers());
        assertEquals(List.of(5, 20), manager.serverIdsInOrder());
        assertEquals(List.of("data api", "database"), manager.serverNamesWithPrefix("data"));
    }

    @Test
    void networkTraversalAndConnectivityUpdateWhenLinksChange() {
        FailureAnalysisService analysis = new FailureAnalysisService();
        analysis.addServer(10);
        analysis.addServer(20);
        analysis.addServer(30);

        assertEquals(List.of(10), analysis.affectedServers(10));
        assertTrue(analysis.connectServers(10, 20));
        assertTrue(analysis.areConnected(10, 20));
        assertFalse(analysis.areConnected(10, 30));
        assertEquals(List.of(10, 20), analysis.affectedServers(10));
    }

    @Test
    void reachabilityResultsCannotMutateFreshOrCachedTraversalResults() {
        FailureAnalysisService analysis = new FailureAnalysisService();
        for (int id : List.of(1, 2, 3, 4)) analysis.addServer(id);
        analysis.connectServers(1, 2);
        analysis.connectServers(1, 3);
        analysis.connectServers(2, 4);
        List<Integer> expected = List.of(1, 2, 3, 4);

        List<Integer> fresh = analysis.affectedServers(1);
        assertEquals(expected, fresh);
        assertThrows(UnsupportedOperationException.class, () -> fresh.add(5));
        assertEquals(expected, analysis.affectedServers(1));

        List<Integer> cached = analysis.affectedServers(1);
        assertThrows(UnsupportedOperationException.class, () -> cached.clear());
        assertEquals(expected, analysis.affectedServers(1));

        analysis.addServer(5);
        assertEquals(expected, analysis.affectedServers(1));
        analysis.connectServers(4, 5);
        assertEquals(List.of(1, 2, 3, 4, 5), analysis.affectedServers(1));
    }

    @Test
    void incidentDequeFeedsThePriorityQueue() {
        RecoveryScheduler scheduler = new RecoveryScheduler();
        RecoveryTask routine = new RecoveryTask(1, 10, 3, 5, "routine repair");
        RecoveryTask urgent = new RecoveryTask(2, 20, 1, 9, "critical repair");

        scheduler.reportIncident(routine, false);
        scheduler.reportIncident(urgent, true);
        assertEquals(urgent, scheduler.dispatchNextIncident());
        assertEquals(routine, scheduler.dispatchNextIncident());
        assertNull(scheduler.dispatchNextIncident());
        assertEquals(urgent, scheduler.nextTask());
        assertEquals(routine, scheduler.nextTask());
        assertNull(scheduler.nextTask());
    }

    @Test
    void telemetryBufferAndSegmentTreeSupportQueueAndRangeOperations() {
        TelemetryManager telemetry = new TelemetryManager(2);
        TelemetryEvent first = new TelemetryEvent(1, 7, "cpu", 40);
        TelemetryEvent second = new TelemetryEvent(2, 7, "memory", 60);

        assertTrue(telemetry.record(first));
        assertTrue(telemetry.record(second));
        assertFalse(telemetry.record(new TelemetryEvent(3, 7, "disk", 20)));
        assertEquals(first, telemetry.readNext());
        assertTrue(telemetry.record(new TelemetryEvent(4, 7, "disk", 20)));

        telemetry.setLoadSamples(new int[]{10, 20, 30, 40});
        assertEquals(90, telemetry.totalLoad(1, 3));
        telemetry.updateLoad(2, 5);
        assertEquals(65, telemetry.totalLoad(1, 3));
    }

    @Test
    void individualStructuresHandleTheirMainOperations() {
        CustomLinkedList<Integer> list = new CustomLinkedList<>();
        list.add(1);
        list.add(2);
        assertTrue(list.removeFirstMatch(1));
        assertEquals(1, list.size());
        assertEquals(2, list.iterator().next());

        CustomQueue<Integer> queue = new CustomQueue<>();
        queue.enqueue(1);
        queue.enqueue(2);
        assertEquals(1, queue.dequeue());
        assertEquals(2, queue.peek());

        CustomStack<Integer> stack = new CustomStack<>();
        stack.push(1);
        stack.push(2);
        assertEquals(2, stack.pop());

        CustomHashTable<Integer, String> table = new CustomHashTable<>();
        for (int index = 0; index < 40; index++) {
            table.put(index, "server-" + index);
        }
        assertEquals("server-35", table.get(35));
        assertEquals(40, table.size());

        BinarySearchTree<Integer> searchTree = new BinarySearchTree<>();
        searchTree.insert(3);
        searchTree.insert(1);
        searchTree.insert(2);
        assertTrue(searchTree.contains(2));
        assertEquals(List.of(1, 2, 3), searchTree.inorder());

        BinaryTree<Integer> tree = new BinaryTree<>();
        BinaryTree.Node<Integer> root = new BinaryTree.Node<>(1);
        root.left = new BinaryTree.Node<>(2);
        root.right = new BinaryTree.Node<>(3);
        tree.setRoot(root);
        assertEquals(List.of(1, 2, 3), tree.preorder());

        DisjointSetUnion groups = new DisjointSetUnion(3);
        groups.union(0, 1);
        assertTrue(groups.connected(0, 1));
        assertFalse(groups.connected(0, 2));

        BinaryHeap<Integer> heap = new BinaryHeap<>(Comparator.naturalOrder());
        heap.add(5);
        heap.add(1);
        assertEquals(1, heap.remove());

        CustomDeque<Integer> deque = new CustomDeque<>();
        deque.addLast(2);
        deque.addFirst(1);
        assertEquals(1, deque.removeFirst());
        assertEquals(2, deque.removeLast());

        CircularQueue<Integer> circularQueue = new CircularQueue<>(1);
        assertTrue(circularQueue.offer(1));
        assertFalse(circularQueue.offer(2));
        assertEquals(1, circularQueue.poll());

        SegmentTree segmentTree = new SegmentTree(new int[]{2, 4, 6});
        assertEquals(6, segmentTree.rangeSum(0, 1));
        segmentTree.update(1, 3);
        assertEquals(11, segmentTree.rangeSum(0, 2));

        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.get(1);
        cache.put(3, "three");
        assertTrue(cache.containsKey(1));
        assertFalse(cache.containsKey(2));

        Trie trie = new Trie();
        trie.insert("Data API");
        trie.insert("Database");
        assertEquals(List.of("data api", "database"), trie.wordsWithPrefix("data"));

        MerkleTree merkleTree = new MerkleTree(List.of("first", "second", "third"));
        List<String> proof = merkleTree.proof(1);
        assertTrue(merkleTree.verify("second", 1, proof, merkleTree.getRoot()));
        assertFalse(merkleTree.verify("changed", 1, proof, merkleTree.getRoot()));
    }
}
