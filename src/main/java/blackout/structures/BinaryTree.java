package blackout.structures;

import java.util.ArrayList;
import java.util.List;

public class BinaryTree<T> {
    public static final class Node<T> {
        public T value;
        public Node<T> left;
        public Node<T> right;

        public Node(T value) {
            this.value = value;
        }
    }

    private Node<T> root;

    public Node<T> getRoot() {
        return root;
    }

    public void setRoot(Node<T> root) {
        this.root = root;
    }

    public List<T> preorder() {
        List<T> values = new ArrayList<>();
        addPreorder(root, values);
        return values;
    }

    public List<T> inorder() {
        List<T> values = new ArrayList<>();
        addInorder(root, values);
        return values;
    }

    public List<T> postorder() {
        List<T> values = new ArrayList<>();
        addPostorder(root, values);
        return values;
    }

    private void addPreorder(Node<T> node, List<T> values) {
        if (node == null) {
            return;
        }
        values.add(node.value);
        addPreorder(node.left, values);
        addPreorder(node.right, values);
    }

    private void addInorder(Node<T> node, List<T> values) {
        if (node == null) {
            return;
        }
        addInorder(node.left, values);
        values.add(node.value);
        addInorder(node.right, values);
    }

    private void addPostorder(Node<T> node, List<T> values) {
        if (node == null) {
            return;
        }
        addPostorder(node.left, values);
        addPostorder(node.right, values);
        values.add(node.value);
    }
}
