package approaches.ngram.table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SimpleHashTable extends FrequencyTable {
    HashMap<String, Integer> frequencies = new HashMap<>();

    public SimpleHashTable(int maxN) {
        super(maxN);
    }

    @Override
    protected void incrementSingle(List<Short> ngram) {
        frequencies.merge(ngramToKey(ngram), 1, Integer::sum);
    }

    @Override
    public int getFrequency(List<Short> ngram) {
        return frequencies.getOrDefault(ngramToKey(ngram), 0);
    }

    private String ngramToKey(List<Short> ngram) {
        StringBuilder sb = new StringBuilder();

        for (short n: ngram) {
            sb.append(n);
            sb.append('|');
        }

        return sb.toString();
    }
    @Override
    public String toString() {
        return frequencies.toString();
    }

    public static void main(String[] args) {
        FrequencyTable table = new SimpleHashTable(3);

        table.incrementAll(Arrays.asList((short) 3, (short) 4, (short) 5));
        System.out.println(table);
        System.out.println(table.getFrequency(Arrays.asList((short) 3, (short) 4, (short) 5)));

        table.incrementAll(Arrays.asList((short) -32768, (short) 0, (short) 32767));
        System.out.println(table);
        System.out.println(table.getFrequency(Arrays.asList((short) -32768, (short) 0, (short) 32767)));

        table.incrementAll(Arrays.asList((short) -8, (short) 0, (short) 32767));
        System.out.println(table);
        System.out.println(table.getFrequency(Arrays.asList((short) 6)));

        table.incrementAll(Arrays.asList((short) 0, (short) 32767, (short) 8));
        System.out.println(table);
    }
}
