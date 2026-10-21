package blackout.structures;
import java.util.*;
public class Trie {
 private static final class Node{Map<Character,Node> children=new TreeMap<>();boolean end;}
 private final Node root=new Node();
 public void insert(String word){if(word==null)throw new IllegalArgumentException("word is null");Node n=root;for(char c:word.toLowerCase(Locale.ROOT).toCharArray())n=n.children.computeIfAbsent(c,k->new Node());n.end=true;}
 public boolean contains(String word){Node n=find(word);return n!=null&&n.end;}
 public List<String> wordsWithPrefix(String prefix){Node n=find(prefix);if(n==null)return List.of();List<String>out=new ArrayList<>();collect(n,new StringBuilder(prefix.toLowerCase(Locale.ROOT)),out);return out;}
 private Node find(String s){if(s==null)return null;Node n=root;for(char c:s.toLowerCase(Locale.ROOT).toCharArray()){n=n.children.get(c);if(n==null)return null;}return n;}
 private void collect(Node n,StringBuilder b,List<String>out){if(n.end)out.add(b.toString());for(var e:n.children.entrySet()){b.append(e.getKey());collect(e.getValue(),b,out);b.setLength(b.length()-1);}}
}
