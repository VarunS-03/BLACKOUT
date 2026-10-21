package blackout.structures;
import java.util.*;
public class BinaryTree<T> {
 public static final class Node<T>{public T value;public Node<T> left,right;public Node(T v){value=v;}}
 private Node<T> root;
 public Node<T> getRoot(){return root;} public void setRoot(Node<T> root){this.root=root;}
 public List<T> preorder(){List<T>o=new ArrayList<>();pre(root,o);return o;}
 private void pre(Node<T>n,List<T>o){if(n==null)return;o.add(n.value);pre(n.left,o);pre(n.right,o);}
 public List<T> inorder(){List<T>o=new ArrayList<>();in(root,o);return o;}
 private void in(Node<T>n,List<T>o){if(n==null)return;in(n.left,o);o.add(n.value);in(n.right,o);}
 public List<T> postorder(){List<T>o=new ArrayList<>();post(root,o);return o;}
 private void post(Node<T>n,List<T>o){if(n==null)return;post(n.left,o);post(n.right,o);o.add(n.value);}
}