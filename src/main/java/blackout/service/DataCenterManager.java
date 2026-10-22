package blackout.service;

import blackout.model.Job;
import blackout.model.Server;
import blackout.structures.BinarySearchTree;
import blackout.structures.BinaryTree;
import blackout.structures.CustomHashTable;
import blackout.structures.CustomLinkedList;
import blackout.structures.CustomQueue;
import blackout.structures.CustomStack;
import blackout.structures.Trie;

import java.util.ArrayList;
import java.util.List;

public class DataCenterManager {
    private final CustomLinkedList<Server> servers = new CustomLinkedList<>();
    private final CustomHashTable<Integer, Server> serversById = new CustomHashTable<>();
    private final BinarySearchTree<Integer> sortedServerIds = new BinarySearchTree<>();
    private final Trie serverNames = new Trie();
    private final CustomQueue<Job> jobs = new CustomQueue<>();
    private final CustomStack<String> configurationHistory = new CustomStack<>();

    private String configuration = "Standard";
    private BinaryTree<Integer> failoverPlan = new BinaryTree<>();

    public boolean registerServer(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("server cannot be null");
        }
        if (serversById.containsKey(server.getId())) {
            return false;
        }

        servers.add(server);
        serversById.put(server.getId(), server);
        sortedServerIds.insert(server.getId());
        serverNames.insert(server.getName());
        return true;
    }

    public Server findServer(int id) {
        return serversById.get(id);
    }

    public List<Server> registeredServers() {
        List<Server> result = new ArrayList<>();
        for (Server server : servers) {
            result.add(server);
        }
        return result;
    }

    public List<Integer> serverIdsInOrder() {
        return sortedServerIds.inorder();
    }

    public List<String> serverNamesWithPrefix(String prefix) {
        return serverNames.wordsWithPrefix(prefix);
    }

    public Job processNextJob() {
        if (jobs.isEmpty()) {
            return null;
        }
        Job job = jobs.dequeue();
        job.setCompleted(true);
        return job;
    }

    public void submitJob(Job job) {
        jobs.enqueue(job);
    }

    public int queuedJobs() {
        return jobs.size();
    }

    public String currentConfiguration() {
        return configuration;
    }

    public void changeConfiguration(String newConfiguration) {
        configurationHistory.push(configuration);
        configuration = newConfiguration;
    }

    public String rollbackConfiguration() {
        if (configurationHistory.isEmpty()) {
            return null;
        }
        configuration = configurationHistory.pop();
        return configuration;
    }

    public void setFailoverPlan(int primaryServerId, Integer firstBackupId, Integer secondBackupId) {
        BinaryTree<Integer> plan = new BinaryTree<>();
        BinaryTree.Node<Integer> root = new BinaryTree.Node<>(primaryServerId);
        if (firstBackupId != null) {
            root.left = new BinaryTree.Node<>(firstBackupId);
        }
        if (secondBackupId != null) {
            root.right = new BinaryTree.Node<>(secondBackupId);
        }
        plan.setRoot(root);
        failoverPlan = plan;
    }

    public List<Integer> failoverPlanPreorder() {
        return failoverPlan.preorder();
    }

    public int serverCount() {
        return servers.size();
    }
}
