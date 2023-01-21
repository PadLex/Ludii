package approaches.ngram;

import java.util.ArrayList;
import java.util.List;

public class GramNode {
    final char joiner = '|';
    final String nan = "Nan";
    final String gram;
    final GramNode parent;
    List<GramNode> children = new ArrayList<>();

    GramNode(String gram, GramNode parent) {
        this.gram = gram;
        this.parent = parent;
        parent.children.add(this);
    }

    GramNode(String gram) {
        this.gram = gram;
        parent = new GramNode(this);
    }

    private GramNode(GramNode root) {
        gram = nan;
        parent = this;
        children.add(root);
    }

    void propagateVerticalNGrams(int maxN, NGramCollection verticalNGrams) {
        addAncestralNGrams(maxN, verticalNGrams);
        for (GramNode child : children) {
            child.propagateVerticalNGrams(maxN, verticalNGrams);
        }
    }
    void addAncestralNGrams(int maxN, NGramCollection verticalNGrams) {
        addAncestralNGrams(1, "", maxN, verticalNGrams);
    }
    private void addAncestralNGrams(int n, String oldGram, int maxN, NGramCollection verticalNGrams) {
        verticalNGrams.increment(gram + oldGram);

        // parent can't be null since the root node circularly refers to its self as parent.
        if (n < maxN) {
            parent.addAncestralNGrams(n + 1, joiner + gram + oldGram, maxN, verticalNGrams);
        }
    }

    void propagateHorizontalNGrams(int maxN, NGramCollection horizontalNGrams) {
        addHorizontalNGrams(maxN, horizontalNGrams);
        for (GramNode child : children) {
            child.propagateHorizontalNGrams(maxN, horizontalNGrams);
        }
    }
    void addHorizontalNGrams(int maxN, NGramCollection horizontalNGrams) {
        for (int n = 1; n <= maxN; n++) {
            gram: for (int start = 0; start < children.size(); start++) {
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < start+n; i++) {
                    if (i >= children.size())
                        break gram;

                    sb.append(children.get(i).gram).append('|');
                }
                if (sb.length() > 0) {
                    horizontalNGrams.increment(sb.substring(0, sb.length()-1));
                }
            }
        }
    }

}
