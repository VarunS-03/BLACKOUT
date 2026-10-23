package blackout.structures;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class MerkleTree {
    private final List<String> leafHashes = new ArrayList<>();
    private final List<List<String>> levels = new ArrayList<>();

    public MerkleTree(List<String> records) {
        for (String record : records) {
            leafHashes.add(hash("leaf:" + record));
        }

        if (leafHashes.isEmpty()) {
            levels.add(List.of(hash("empty-tree")));
            return;
        }

        List<String> currentLevel = List.copyOf(leafHashes);
        levels.add(currentLevel);
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int index = 0; index < currentLevel.size(); index += 2) {
                String leftHash = currentLevel.get(index);
                String rightHash = index + 1 < currentLevel.size()
                        ? currentLevel.get(index + 1)
                        : leftHash;
                nextLevel.add(hash("node:" + leftHash + rightHash));
            }
            currentLevel = List.copyOf(nextLevel);
            levels.add(currentLevel);
        }
    }

    public String getRoot() {
        List<String> rootLevel = levels.get(levels.size() - 1);
        return rootLevel.get(0);
    }

    public List<String> proof(int index) {
        if (index < 0 || index >= leafHashes.size()) {
            throw new IndexOutOfBoundsException("record index " + index);
        }

        List<String> proof = new ArrayList<>();
        int nodeIndex = index;
        for (int level = 0; level < levels.size() - 1; level++) {
            List<String> row = levels.get(level);
            int siblingIndex = nodeIndex ^ 1;
            if (siblingIndex >= row.size()) {
                siblingIndex = nodeIndex;
            }

            String side = siblingIndex < nodeIndex ? "L:" : "R:";
            proof.add(side + row.get(siblingIndex));
            nodeIndex /= 2;
        }
        return proof;
    }

    public boolean verify(String record, int index, List<String> proof, String trustedRoot) {
        if (index < 0 || index >= leafHashes.size()
                || proof == null || trustedRoot == null
                || proof.size() != levels.size() - 1) {
            return false;
        }

        String currentHash = hash("leaf:" + record);
        int nodeIndex = index;
        for (String proofPart : proof) {
            if (proofPart.length() < 3) {
                return false;
            }

            String side = proofPart.substring(0, 2);
            String siblingHash = proofPart.substring(2);
            if (side.equals("L:")) {
                if (nodeIndex % 2 == 0) {
                    return false;
                }
                currentHash = hash("node:" + siblingHash + currentHash);
            } else if (side.equals("R:")) {
                if (nodeIndex % 2 != 0) {
                    return false;
                }
                currentHash = hash("node:" + currentHash + siblingHash);
            } else {
                return false;
            }
            nodeIndex /= 2;
        }
        return nodeIndex == 0 && currentHash.equals(trustedRoot);
    }

    public static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
