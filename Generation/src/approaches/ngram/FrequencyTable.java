package approaches.ngram;

import java.util.List;

public abstract class FrequencyTable {
    public final int maxN;
    protected int total;

    protected FrequencyTable(int maxN) {
        this.maxN = maxN;
    }

    public void incrementAll(List<String> ngram) {
        if (ngram.size() != maxN) {
            throw new IllegalArgumentException("ngram must be of size maxN");
        }

        total++;
        incrementRecursively(ngram);
    }
    private void incrementRecursively(List<String> ngram) {
        incrementSingle(ngram);
        int n = ngram.size();
        if (n > 1) {
            incrementRecursively(ngram.subList(1, n));
        }
    }
    protected abstract void incrementSingle(List<String> ngram);
    abstract int getFrequency(List<String> ngram);
    public int getTotal() {
        return total;
    }
}
