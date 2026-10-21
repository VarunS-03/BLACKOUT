package blackout.structures;
import java.util.Iterator;
import java.util.NoSuchElementException;
public class CustomLinkedList<T> implements Iterable<T> {
    private static final class Node<T> { T value; Node<T> next; Node(T v){value=v;} }
    private Node<T> head, tail; private int size;
    public void add(T value) { Node<T> n=new Node<>(value); if(tail==null) head=n; else tail.next=n; tail=n; size++; }
    public boolean removeFirstMatch(T value) {
        Node<T> prev=null, cur=head;
        while(cur!=null) { if(java.util.Objects.equals(cur.value,value)) { if(prev==null) head=cur.next; else prev.next=cur.next; if(cur==tail) tail=prev; size--; return true; } prev=cur; cur=cur.next; }
        return false;
    }
    public int size(){return size;} public boolean isEmpty(){return size==0;}
    @Override public Iterator<T> iterator(){ return new Iterator<>() { Node<T> n=head; @Override public boolean hasNext(){return n!=null;} @Override public T next(){if(n==null)throw new NoSuchElementException(); T v=n.value;n=n.next;return v;} }; }
}