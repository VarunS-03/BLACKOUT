package blackout.service;

import blackout.structures.DisjointSetUnion;
import blackout.structures.Graph;
import blackout.structures.LRUCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FailureAnalysisService {
    private final Graph network = new Graph();
    private final Map<Integer, Integer> indexByServerId = new HashMap<>();
    private DisjointSetUnion connectedGroups = new DisjointSetUnion(0);
    private LRUCache<Integer, List<Integer>> affectedServerCache = new LRUCache<>(10);

    public boolean addServer(int serverId) {
        if (network.vertices().contains(serverId)) {
            return false;
        }
        network.addVertex(serverId);
        rebuildConnectedGroups();
        affectedServerCache = new LRUCache<>(10);
        return true;
    }

    public boolean connectServers(int firstServerId, int secondServerId) {
        if (!network.vertices().contains(firstServerId)
                || !network.vertices().contains(secondServerId)
                || firstServerId == secondServerId
                || network.neighbors(firstServerId).contains(secondServerId)) {
            return false;
        }

        network.addEdge(firstServerId, secondServerId);
        rebuildConnectedGroups();
        affectedServerCache = new LRUCache<>(10);
        return true;
    }

    public List<Integer> affectedServers(int failedServerId) {
        List<Integer> cachedResult = affectedServerCache.get(failedServerId);
        if (cachedResult != null) {
            return cachedResult;
        }

        List<Integer> result = network.bfs(failedServerId);
        affectedServerCache.put(failedServerId, result);
        return result;
    }

    public boolean areConnected(int firstServerId, int secondServerId) {
        Integer firstIndex = indexByServerId.get(firstServerId);
        Integer secondIndex = indexByServerId.get(secondServerId);
        if (firstIndex == null || secondIndex == null) {
            return false;
        }
        return connectedGroups.connected(firstIndex, secondIndex);
    }

    public Set<Integer> neighbors(int serverId) {
        return network.neighbors(serverId);
    }

    public Set<Integer> serverIds() {
        return network.vertices();
    }

    private void rebuildConnectedGroups() {
        List<Integer> serverIds = new ArrayList<>(network.vertices());
        indexByServerId.clear();
        for (int index = 0; index < serverIds.size(); index++) {
            indexByServerId.put(serverIds.get(index), index);
        }

        connectedGroups = new DisjointSetUnion(serverIds.size());
        for (int serverId : serverIds) {
            for (int neighborId : network.neighbors(serverId)) {
                if (serverId < neighborId) {
                    connectedGroups.union(
                            indexByServerId.get(serverId),
                            indexByServerId.get(neighborId));
                }
            }
        }
    }
}
