package approaches.ngram.table;

import java.util.HashMap;
import java.util.List;

public class SimpleHashTable extends FrequencyTable {
    HashMap<String, Integer> frequencies = new HashMap<>();

    public SimpleHashTable(int maxN) {
        super(maxN);
    }

    @Override
    protected void incrementSingle(List<String> ngram) {
        frequencies.merge(ngramToString(ngram), 1, Integer::sum);
    }

    @Override
    public int getFrequency(List<String> ngram) {
        return frequencies.getOrDefault(ngramToString(ngram), 0);
    }

    private String ngramToString(List<String> ngram) {
        return ngram.stream().reduce((s1, s2) -> s1 + '|' + s2).orElseThrow();
    }
    @Override
    public String toString() {
        return frequencies.toString();
    }
}
