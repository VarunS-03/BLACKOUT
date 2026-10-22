package blackout.structures;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {
    private final Map<Integer, Set<Integer>> adjacencyList = new LinkedHashMap<>();

    public void addVertex(int vertex) {
        adjacencyList.computeIfAbsent(vertex, ignored -> new LinkedHashSet<>());
    }

    public void addEdge(int first, int second) {
        addVertex(first);
        addVertex(second);
        adjacencyList.get(first).add(second);
        adjacencyList.get(second).add(first);
    }

    public Set<Integer> neighbors(int vertex) {
        Set<Integer> connectedVertices = adjacencyList.get(vertex);
        if (connectedVertices == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(connectedVertices);
    }

    public List<Integer> bfs(int start) {
        if (!adjacencyList.containsKey(start)) {
            return List.of();
        }

        List<Integer> visitOrder = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> waiting = new ArrayDeque<>();
        waiting.add(start);
        visited.add(start);

        while (!waiting.isEmpty()) {
            int vertex = waiting.remove();
            visitOrder.add(vertex);
            for (int neighbor : adjacencyList.get(vertex)) {
                if (visited.add(neighbor)) {
                    waiting.add(neighbor);
                }
            }
        }
        return visitOrder;
    }

    public List<Integer> dfs(int start) {
        List<Integer> visitOrder = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        visitDepthFirst(start, visited, visitOrder);
        return visitOrder;
    }

    public Set<Integer> reachable(int start) {
        return new LinkedHashSet<>(bfs(start));
    }

    public boolean removeEdge(int first, int second) {
        Set<Integer> firstNeighbors = adjacencyList.get(first);
        Set<Integer> secondNeighbors = adjacencyList.get(second);
        if (firstNeighbors == null || secondNeighbors == null) {
            return false;
        }

        boolean removed = firstNeighbors.remove(second);
        if (first != second) {
            secondNeighbors.remove(first);
        }
        return removed;
    }

    public Set<Integer> vertices() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    private void visitDepthFirst(int vertex, Set<Integer> visited, List<Integer> visitOrder) {
        if (!adjacencyList.containsKey(vertex) || !visited.add(vertex)) {
            return;
        }

        visitOrder.add(vertex);
        for (int neighbor : adjacencyList.get(vertex)) {
            visitDepthFirst(neighbor, visited, visitOrder);
        }
    }
}
