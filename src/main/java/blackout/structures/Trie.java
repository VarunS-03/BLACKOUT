package blackout.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Trie {
    private static final class Node {
        private final Map<Character, Node> children = new TreeMap<>();
        private boolean wordEnd;
    }

    private final Node root = new Node();

    public void insert(String word) {
        if (word == null) {
            throw new IllegalArgumentException("word cannot be null");
        }

        Node current = root;
        for (char letter : word.toLowerCase(Locale.ROOT).toCharArray()) {
            current = current.children.computeIfAbsent(letter, ignored -> new Node());
        }
        current.wordEnd = true;
    }

    public boolean contains(String word) {
        Node node = findNode(word);
        return node != null && node.wordEnd;
    }

    public List<String> wordsWithPrefix(String prefix) {
        Node prefixNode = findNode(prefix);
        if (prefixNode == null) {
            return List.of();
        }

        List<String> words = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder(prefix.toLowerCase(Locale.ROOT));
        collectWords(prefixNode, currentWord, words);
        return words;
    }

    private Node findNode(String text) {
        if (text == null) {
            return null;
        }

        Node current = root;
        for (char letter : text.toLowerCase(Locale.ROOT).toCharArray()) {
            current = current.children.get(letter);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private void collectWords(Node node, StringBuilder currentWord, List<String> words) {
        if (node.wordEnd) {
            words.add(currentWord.toString());
        }
        for (Map.Entry<Character, Node> child : node.children.entrySet()) {
            currentWord.append(child.getKey());
            collectWords(child.getValue(), currentWord, words);
            currentWord.setLength(currentWord.length() - 1);
        }
    }
}
