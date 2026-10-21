package blackout.service;
import blackout.model.AuditRecord;
import blackout.structures.MerkleTree;
import java.util.*;
public class AuditLogger {
 private final List<AuditRecord> records=new ArrayList<>();
 public AuditRecord log(String action,String details){AuditRecord r=new AuditRecord(records.size()+1L,action,details);records.add(r);return r;}
 public List<AuditRecord> records(){return Collections.unmodifiableList(records);}
 public String merkleRoot(){List<String> canonical=new ArrayList<>();for(AuditRecord r:records)canonical.add(r.canonicalForm());return new MerkleTree(canonical).getRoot();}
 public boolean verifyRecord(int zeroBasedIndex,String candidate,String trustedRoot){List<String>canonical=new ArrayList<>();for(AuditRecord r:records)canonical.add(r.canonicalForm());MerkleTree tree=new MerkleTree(canonical);return tree.verify(candidate,zeroBasedIndex,tree.proof(zeroBasedIndex),trustedRoot);}
}
