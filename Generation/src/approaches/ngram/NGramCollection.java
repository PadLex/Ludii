package approaches.ngram;

import java.util.Iterator;

public interface NGramCollection {
    void increment(String ngram);
    int getCount(String ngram);
}
