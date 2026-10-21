package blackout.cli;
import blackout.model.AuditRecord;
import blackout.service.AuditLogger;
import blackout.structures.*;
import java.util.*;
public class Main {
 public static void main(String[] args){
  System.out.println("BLACKOUT / Member 4 integrity demo");
  AuditLogger log=new AuditLogger();log.log("SERVER_DOWN","server=12");log.log("RECOVERY_STARTED","task=4");
  String trusted=log.merkleRoot();System.out.println("Audit records: "+log.records().size());
  System.out.println("Trusted Merkle root: "+trusted);
  System.out.println("Root unchanged after clean verification: "+log.verifyRecord(0,log.records().get(0).canonicalForm(),trusted));
  MerkleTree tree=new MerkleTree(List.of("alpha","beta","gamma"));System.out.println("Tampered record accepted? "+tree.verify("BETA",1,tree.proof(1),tree.getRoot()));
  LRUCache<Integer,String> cache=new LRUCache<>(2);cache.put(1,"one");cache.put(2,"two");cache.get(1);cache.put(3,"three");System.out.println("LRU kept key 1? "+cache.containsKey(1)+", evicted key 2? "+!cache.containsKey(2));
  Trie trie=new Trie();trie.insert("database");trie.insert("data-api");System.out.println("Prefix 'data': "+trie.wordsWithPrefix("data"));
 }
}
