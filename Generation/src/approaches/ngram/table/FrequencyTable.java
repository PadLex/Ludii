package approaches.ngram.table;

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

        if (ngram.size() > 1) {
            incrementRecursively(ngram.subList(1, ngram.size()));
        }
    }
    protected abstract void incrementSingle(List<String> ngram);
    public abstract int getFrequency(List<String> ngram);
    public int getTotal() {
        return total;
    }
    public double stupidBackoffScore(List<String> ngram, double discount) {
        if (ngram.size() == 1) {
            return getFrequency(ngram) / (double) getTotal();
        }

        int thisFrequency = getFrequency(ngram);
        if (thisFrequency == 0)
            return discount * stupidBackoffScore(ngram.subList(1, ngram.size()), discount);

        int parentFrequency = getFrequency(ngram.subList(0, ngram.size()-1));

        System.out.println(thisFrequency + " / " + parentFrequency + " -> " + thisFrequency / (double) parentFrequency);

        return thisFrequency / (double) parentFrequency;
    }
}
