package approaches.ngram;

import java.util.HashMap;

public class SimpleHashTable implements FrequencyTable {
    HashMap<Integer, Integer> counts = new HashMap<>();
    HashMap<String, Integer> frequencies = new HashMap<>();

    @Override
    public void increment(String ngram, int n) {
        counts.merge(n, 1, Integer::sum);
        frequencies.merge(ngram, 1, Integer::sum);
    }

    @Override
    public int getFrequency(String ngram) {
        return frequencies.getOrDefault(ngram, 0);
    }

    @Override
    public int getCount(int n) {
        return frequencies.getOrDefault(n, 0);
    }

    @Override
    public String toString() {
        return frequencies.toString();
    }
}
