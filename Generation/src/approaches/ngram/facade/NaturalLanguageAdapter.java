package approaches.ngram.facade;

import approaches.ngram.table.FrequencyTable;
import approaches.ngram.table.SimpleHashTable;
import approaches.ngram.table.SimpleTrie;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class NaturalLanguageAdapter {

    String startGram = "<s>";
    String endGram = "</s>";

    FrequencyTable frequencyTable = new SimpleTrie(5);

    HashSet<String> dictionary;

    Random random = new Random();

    NaturalLanguageAdapter() {
        dictionary = new HashSet<>();
        dictionary.add(endGram);
    }

    public void addText(String str) {
        List<String> gramSequence = new ArrayList<>(Arrays.asList(str.split(" ")));
        dictionary.addAll(gramSequence);
        for (int i = 0; i < frequencyTable.maxN; i++) {
            gramSequence.add(0, startGram);
        }
        gramSequence.add(endGram);
        gramSequence = gramSequence.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();

        for (int i = 0; i <= gramSequence.size() - frequencyTable.maxN; i++) {
            frequencyTable.incrementAll(gramSequence.subList(i, i + frequencyTable.maxN));
        }
    }

    public String predictNextGram(List<String> gramSequence) {
        List<String> ngram = new ArrayList<>(gramSequence);

        if (ngram.size() > frequencyTable.maxN) {
            ngram = ngram.subList(ngram.size() - frequencyTable.maxN + 1, ngram.size());
        }
        else if (ngram.size() < frequencyTable.maxN) {
            int max = frequencyTable.maxN - ngram.size() - 1;
            for (int i = 0; i < max; i++) {
                ngram.add(0, startGram);
            }
        }
        else {
            ngram = ngram.subList(1, ngram.size());
        }

        //System.out.println(ngram.size() + " " + frequencyTable.maxN + " " + ngram);

        HashMap<String, Double> options = new HashMap<>();
        double sum = 0;
        for (String gram: dictionary) {
            ngram.add(gram);

            double score = frequencyTable.stupidBackoffScore(ngram, 0.4);

            if (Double.isInfinite(score) || Double.isNaN(score) || score < 0)
                throw new Error("Double underflow, plz fix: " + ngram + " -> " + score);

            if (score > 0) {
                options.put(gram, score);
                sum += score;
            }

            ngram.remove(ngram.size() - 1);
        }

        //System.out.println(sum);


        ArrayList<Map.Entry<String, Double>> sortedOptions = new ArrayList<>(options.entrySet());
        sortedOptions.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        List<String> grams = sortedOptions.stream().map(Map.Entry::getKey).toList();
        double finalSum = sum;
        List<Double> probabilities = sortedOptions.stream().map(Map.Entry::getValue).map(p -> p/finalSum).toList();


        double x = random.nextDouble();
        double cumulativeSum = 0;
        for (int i = 0; i < probabilities.size(); i++) {
            cumulativeSum += probabilities.get(i);

            if (x <= cumulativeSum)
                return grams.get(i);
        }

        throw new Error("Impossible state. Cumulative probability must be <= 1 but it is " + cumulativeSum);
    }

    public void addTextFile(String fileName) {
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.map(String::strip).filter(s -> !s.isEmpty()).forEach(this::addText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generationTest(String fileName, int outputLength) {
        NaturalLanguageAdapter nlGenerator = new NaturalLanguageAdapter();

        nlGenerator.addTextFile(fileName);

        List<String> sentence = new ArrayList<>(Arrays.asList());
        for (int i = 0; i < outputLength; i++) {
            String nexGram = nlGenerator.predictNextGram(sentence);
            sentence.add(nexGram);

            if (nexGram.equals(nlGenerator.endGram)) {
                System.out.print('\n');
                sentence.clear();
            } else {
                if (nexGram.matches("\\w*")) {
                    System.out.print(' ');
                }
                System.out.print(nexGram);
            }

        }
    }

    public static void main(String[] args) throws IOException {
        generationTest("/Users/alex/Documents/Marble/Random Text/shakespeare.txt", 200);
    }
}
