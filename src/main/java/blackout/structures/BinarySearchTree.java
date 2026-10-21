package blackout.structures;
import java.util.*;
public class BinarySearchTree<T extends Comparable<T>> {
 private static final class Node<T>{T v;Node<T>l,r;Node(T v){this.v=v;}}
 private Node<T> root;
 public void insert(T v){root=insert(root,v);}
 private Node<T> insert(Node<T>n,T v){if(n==null)return new Node<>(v);int c=v.compareTo(n.v);if(c<0)n.l=insert(n.l,v);else if(c>0)n.r=insert(n.r,v);return n;}
 public boolean contains(T v){Node<T>n=root;while(n!=null){int c=v.compareTo(n.v);if(c==0)return true;n=c<0?n.l:n.r;}return false;}
 public List<T> inorder(){List<T>o=new ArrayList<>();walk(root,o);return o;}
 private void walk(Node<T>n,List<T>o){if(n==null)return;walk(n.l,o);o.add(n.v);walk(n.r,o);}
}