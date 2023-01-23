package approaches.ngram;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class GramNode {
    static final char joiner = '|';
    static final String nan = "Nan";
    private final int maxN;
    private String gram;
    private GramNode parent;
    private List<GramNode> children = new LinkedList<>();
    private String verticalNgrams;
    private Collection<String> horizontalNgrams;

    // root node
    GramNode(String gram, int maxN) {
        this.gram = gram;
        this.maxN = maxN;
    }

    // Normal node
    GramNode(String gram, GramNode parent) {
        this.gram = gram;
        this.parent = parent;
        this.maxN = parent.maxN;
        parent.children.add(this);
    }

    void recursivelyIncrementNgrams(FrequencyTable verticalFrequencyTable, FrequencyTable horizontalFrequencyTable) {
        for (String ngram: getVerticalNGrams()) {
            verticalFrequencyTable.increment(ngram);
        }

        for (String ngram: getHorizontalNGrams()) {
            horizontalFrequencyTable.increment(ngram);
        }

        for (GramNode child : children) {
            child.recursivelyIncrementNgrams(verticalFrequencyTable, horizontalFrequencyTable);
        }
    }
    private Collection<String> getVerticalNGrams() {
        if (verticalNgrams == null) {
            verticalNgrams = new LinkedList<>();
            findVerticalNGrams(1, "", verticalNgrams);
        }

        return verticalNgrams;
    }
    private void findVerticalNGrams(int n, String oldGram, Collection<String> verticalNGrams) {
        verticalNGrams.add(gram + oldGram);

        if (n < maxN && parent != null) {
            parent.findVerticalNGrams(n + 1, joiner + gram + oldGram, verticalNGrams);
        }
    }
    public Collection<String> getHorizontalNGrams() {
        if (horizontalNgrams != null) {
            return horizontalNgrams;
        }

        horizontalNgrams = new LinkedList<>();
        for (int n = 1; n <= maxN; n++) {
            gram: for (int start = 0; start < children.size(); start++) {
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < start+n; i++) {
                    if (i >= children.size())
                        break gram;

                    sb.append(children.get(i).gram).append('|');
                }
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                    horizontalNgrams.add(sb.toString());
                }
            }
        }

        return horizontalNgrams;
    }

    public float verticalLogProbability(FrequencyTable verticalFrequencyTable) {
        return verticalFrequencyTable.getFrequency(verticalNgrams);
    }
}
