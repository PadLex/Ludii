package approaches.ngram;

import java.util.HashMap;

public class SimpleCollection implements NGramCollection{
    HashMap<String, Integer> counts = new HashMap<>();

    @Override
    public void increment(String ngram) {
        counts.merge(ngram, 1, Integer::sum);
    }

    @Override
    public int getCount(String ngram) {
        return counts.getOrDefault(ngram, 0);
    }

    @Override
    public String toString() {
        return counts.toString();
    }
}
