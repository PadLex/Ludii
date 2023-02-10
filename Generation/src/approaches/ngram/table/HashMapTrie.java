package approaches.ngram.table;

import java.util.HashMap;
import java.util.List;

public class HashMapTrie extends FrequencyTable {
    Node root = new Node();
    public HashMapTrie(int maxN) {
        super(maxN);
    }
    @Override
    protected void incrementSingle(List<Integer> ngram) {
        root.increment(ngram);
    }
    @Override
    public int getFrequency(List<Integer> ngram) {
        return root.getFrequency(ngram);
    }
    @Override
    public String toString() {
        return root.toString();
    }

    private class Node {
        int count;
        HashMap<Integer, Node> children = new HashMap<>();

        public Node() {
        }
        public Node(int gram, Node parent) {
            parent.children.put(gram, this);
        }

        void increment(List<Integer> ngram) {
            if (ngram.size() == 0) {
                count++;
                return;
            }

            Node child = children.get(ngram.get(0));
            if (child == null)
                child = new Node(ngram.get(0), this);

            child.increment(ngram.subList(1, ngram.size()));
        }
        int getFrequency(List<Integer> ngram) {
            if (ngram.size() == 0)
                return count;

            Node child = children.get(ngram.get(0));
            if (child != null) {
                return child.getFrequency(ngram.subList(1, ngram.size()));
            }

            return 0;
        }

        /*
        HashMap<Integer, Integer> getCounts() {
            HashMap<Integer, Integer> counts = new HashMap<>();
            getCounts(counts, "");
            return counts;
        }

        void getCounts(HashMap<String, Integer> counts, int[] ngram) {
            if (count > 0)
                counts.put(ngram.substring(0, ngram.length()-1), count);

            if (children.size() > 0) {
                for (Map.Entry<String, Node> childEntry : children.entrySet()) {
                    childEntry.getValue().getCounts(counts, ngram + childEntry.getKey() + '|');
                }
            }
        }

        @Override
        public String toString() {
            return getCounts().toString();
        }*/
    }
}
