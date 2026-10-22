package blackout.structures;

import java.util.ArrayList;
import java.util.List;

public class BinarySearchTree<T extends Comparable<T>> {
    private static final class Node<T> {
        private final T value;
        private Node<T> left;
        private Node<T> right;

        private Node(T value) {
            this.value = value;
        }
    }

    private Node<T> root;

    public void insert(T value) {
        root = insert(root, value);
    }

    public boolean contains(T value) {
        Node<T> current = root;
        while (current != null) {
            int comparison = value.compareTo(current.value);
            if (comparison == 0) {
                return true;
            }
            current = comparison < 0 ? current.left : current.right;
        }
        return false;
    }

    public List<T> inorder() {
        List<T> values = new ArrayList<>();
        addInorder(root, values);
        return values;
    }

    private Node<T> insert(Node<T> node, T value) {
        if (node == null) {
            return new Node<>(value);
        }

        int comparison = value.compareTo(node.value);
        if (comparison < 0) {
            node.left = insert(node.left, value);
        } else if (comparison > 0) {
            node.right = insert(node.right, value);
        }
        return node;
    }

    private void addInorder(Node<T> node, List<T> values) {
        if (node == null) {
            return;
        }
        addInorder(node.left, values);
        values.add(node.value);
        addInorder(node.right, values);
    }
}
