package approaches.ngram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class FrequencyTable {
    abstract void increment(List<String> ngram);
    abstract int frequency(List<String> ngram);
    abstract int count(int n);
    double conditionalLogProbability(List<String> ngram, String nextGram) {
        List<String> nextNgram = new ArrayList<>(ngram);
        nextNgram.add(nextGram);

        return Math.log(frequency(nextNgram)) - Math.log(frequency(ngram));
    }
}
