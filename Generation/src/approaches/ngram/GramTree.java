package approaches.ngram;

import java.util.HashMap;
import java.util.List;

public abstract class GramTree <T> {
    GramNode root;
    HashMap<T, GramNode> nodes = new HashMap<>();

    abstract void setNodeTree(T rootToken);
    abstract String toGram(T token);
    void incrementAll(int maxN, NGramCollection verticalNGrams, NGramCollection horizontalNGrams) {
        root.propagateVerticalNGrams(maxN, verticalNGrams);
        root.propagateHorizontalNGrams(maxN, horizontalNGrams);
    }
    void incrementFrom(T startToken, int maxN, NGramCollection verticalNGrams, NGramCollection horizontalNGrams) {
        GramNode node = nodes.get(startToken);
        node.propagateVerticalNGrams(maxN, verticalNGrams);
        node.propagateHorizontalNGrams(maxN, horizontalNGrams);
    }



}
