package approaches.ngram.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public HashMap<List<Integer>, Integer> getFrequencies() {
        HashMap<List<Integer>, Integer> counts = new HashMap<>();
        root.getFrequencies(counts, new ArrayList<>());
        return counts;
    }

    @Override
    public String toString() {
        return getFrequencies().toString();
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

        void getFrequencies(HashMap<List<Integer>, Integer> counts, List<Integer> ngram) {
            if (count > 0)
                counts.put(new ArrayList<>(ngram.subList(0, ngram.size()-1)), count);

            if (children.size() > 0) {
                for (Map.Entry<Integer, Node> childEntry : children.entrySet()) {
                    ngram.add(childEntry.getKey());
                    childEntry.getValue().getFrequencies(counts, ngram);
                    ngram.remove(ngram.size()-1);
                }
            }
        }
    }
}
