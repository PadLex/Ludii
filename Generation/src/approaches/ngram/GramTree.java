package approaches.ngram;

public class GramTree {
    GramNode root;

    public GramTree(String rootGram) {
        this.root = new GramNode(rootGram);
    }

    void incrementAll(int maxN, FrequencyTable verticalNGrams, FrequencyTable horizontalNGrams) {
        root.propagateVerticalNGrams(maxN, verticalNGrams);
        root.propagateHorizontalNGrams(maxN, horizontalNGrams);
    }
    void incrementFrom(GramNode node, int maxN, FrequencyTable verticalNGrams, FrequencyTable horizontalNGrams) {
        node.propagateVerticalNGrams(maxN, verticalNGrams);
        node.propagateHorizontalNGrams(maxN, horizontalNGrams);
    }





}
