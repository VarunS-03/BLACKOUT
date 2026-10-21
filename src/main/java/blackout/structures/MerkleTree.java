package blackout.structures;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
public class MerkleTree {
 private final List<String> leaves;private final List<List<String>> levels=new ArrayList<>();
 public MerkleTree(List<String> records){
  leaves=new ArrayList<>();for(String r:records)leaves.add(hash("leaf:"+r));
  if(leaves.isEmpty())levels.add(List.of(hash("empty-tree")));
  else {levels.add(List.copyOf(leaves));List<String> cur=leaves;
   while(cur.size()>1){List<String> next=new ArrayList<>();for(int i=0;i<cur.size();i+=2){String left=cur.get(i),right=(i+1<cur.size()?cur.get(i+1):left);next.add(hash("node:"+left+right));}levels.add(List.copyOf(next));cur=next;}
  }
 }
 public String getRoot(){return levels.get(levels.size()-1).get(0);}
 public List<String> proof(int index){if(index<0||index>=leaves.size())throw new IndexOutOfBoundsException();List<String>p=new ArrayList<>();int i=index;for(int level=0;level<levels.size()-1;level++){List<String> row=levels.get(level);int sibling=i^1;if(sibling>=row.size())sibling=i;p.add((sibling<i?"L:":"R:")+row.get(sibling));i/=2;}return p;}
 public boolean verify(String record,int index,List<String> proof,String trustedRoot){String cur=hash("leaf:"+record);int i=index;for(String item:proof){if(item.length()<3)return false;String side=item.substring(0,2),sibling=item.substring(2);cur=side.equals("L:")?hash("node:"+sibling+cur):side.equals("R:")?hash("node:"+cur+sibling):"";if(cur.isEmpty())return false;i/=2;}return cur.equals(trustedRoot);}
 public static String hash(String text){try{byte[]b=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(b);}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
