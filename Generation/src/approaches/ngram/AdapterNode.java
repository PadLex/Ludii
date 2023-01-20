package approaches.ngram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class AdapterNode {
    final char joiner = '|';
    final String nan = "Nan";
    final String gram;
    final AdapterNode parent;
    Collection<AdapterNode> children = new HashSet<>();

    final NGramCollection verticalNGrams;
    final NGramCollection horizontalNGrams;

    AdapterNode(String gram, AdapterNode parent) {
        this.gram = gram;
        this.parent = parent;
        parent.children.add(this);
        verticalNGrams = parent.verticalNGrams;
        horizontalNGrams = parent.horizontalNGrams;
    }

    AdapterNode(String gram, NGramCollection verticalNGrams, NGramCollection horizontalNGrams) {
        this.gram = gram;
        this.verticalNGrams = verticalNGrams;
        this.horizontalNGrams = horizontalNGrams;
        parent = new AdapterNode(this);
    }

    private AdapterNode(AdapterNode root) {
        gram = nan;
        parent = this;
        children.add(root);
        verticalNGrams = root.verticalNGrams;
        horizontalNGrams = root.horizontalNGrams;
    }

    void propagateVerticalNGrams(int maxN) {
        addAncestralNGrams(maxN);
        for (AdapterNode child : children) {
            child.propagateVerticalNGrams(maxN);
        }
    }
    void addAncestralNGrams(int maxN) {
        addAncestralNGrams(1, "", maxN);
    }
    private void addAncestralNGrams(int n, String oldGram, int maxN) {
        verticalNGrams.increment(gram + oldGram);

        // parent can't be null since the root node circularly refers to its self as parent.
        if (n < maxN) {
            parent.addAncestralNGrams(n + 1, joiner + gram + oldGram, maxN);
        }
    }

}
