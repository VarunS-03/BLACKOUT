package blackout.structures;
import java.util.*;
public class Graph {
 private final Map<Integer,Set<Integer>> adj=new LinkedHashMap<>();
 public void addVertex(int v){adj.computeIfAbsent(v,k->new LinkedHashSet<>());}
 public void addEdge(int a,int b){addVertex(a);addVertex(b);adj.get(a).add(b);adj.get(b).add(a);}
 public Set<Integer> neighbors(int v){return Collections.unmodifiableSet(adj.getOrDefault(v,Set.of()));}
 public List<Integer> bfs(int start){if(!adj.containsKey(start))return List.of();List<Integer> out=new ArrayList<>();Set<Integer> seen=new HashSet<>();ArrayDeque<Integer> q=new ArrayDeque<>();q.add(start);seen.add(start);while(!q.isEmpty()){int v=q.remove();out.add(v);for(int n:adj.get(v))if(seen.add(n))q.add(n);}return out;}
 public List<Integer> dfs(int start){List<Integer> out=new ArrayList<>();Set<Integer> seen=new HashSet<>();dfsVisit(start,seen,out);return out;}
 private void dfsVisit(int v,Set<Integer>s,List<Integer>o){if(!adj.containsKey(v)||!s.add(v))return;o.add(v);for(int n:adj.get(v))dfsVisit(n,s,o);}
 public Set<Integer> reachable(int start){return new LinkedHashSet<>(bfs(start));}
 public boolean removeEdge(int a,int b){if(!adj.containsKey(a)||!adj.containsKey(b))return false;boolean removed=adj.get(a).remove(b);if(a!=b)adj.get(b).remove(a);return removed;}
 public Set<Integer> vertices(){return Collections.unmodifiableSet(adj.keySet());}
}