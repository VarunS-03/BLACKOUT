package blackout.structures;
import java.util.NoSuchElementException;
public class CustomQueue<T> {
    private static final class Node<T>{T v;Node<T> n;Node(T v){this.v=v;}}
    private Node<T> head,tail; private int size;
    public void enqueue(T v){Node<T>x=new Node<>(v);if(tail==null)head=x;else tail.n=x;tail=x;size++;}
    public T dequeue(){if(head==null)throw new NoSuchElementException("Queue empty");T v=head.v;head=head.n;if(head==null)tail=null;size--;return v;}
    public T peek(){if(head==null)throw new NoSuchElementException("Queue empty");return head.v;}
    public int size(){return size;} public boolean isEmpty(){return size==0;}
}