package blackout.structures;
public class DisjointSetUnion {
 private final int[] parent,rank;
 public DisjointSetUnion(int n){if(n<0)throw new IllegalArgumentException();parent=new int[n];rank=new int[n];for(int i=0;i<n;i++)parent[i]=i;}
 public int find(int x){check(x);if(parent[x]!=x)parent[x]=find(parent[x]);return parent[x];}
 public boolean union(int a,int b){int ra=find(a),rb=find(b);if(ra==rb)return false;if(rank[ra]<rank[rb])parent[ra]=rb;else if(rank[ra]>rank[rb])parent[rb]=ra;else{parent[rb]=ra;rank[ra]++;}return true;}
 public boolean connected(int a,int b){return find(a)==find(b);}
 private void check(int x){if(x<0||x>=parent.length)throw new IndexOutOfBoundsException("node "+x);}
}