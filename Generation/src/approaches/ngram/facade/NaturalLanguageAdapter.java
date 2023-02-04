package approaches.ngram.facade;

import approaches.ngram.table.FrequencyTable;
import approaches.ngram.table.SimpleHashTable;
import approaches.ngram.table.SimpleTrie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class NaturalLanguageAdapter {

    String startGram = "<s>";
    String endGram = "</s>";

    FrequencyTable frequencyTable = new SimpleTrie(6);

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

            double score = frequencyTable.stupidBackoffScore(ngram, 0.8);

            if (Double.isInfinite(score) || Double.isNaN(score) || score < 0)
                throw new Error("Double underflow, plz fix: " + ngram + " -> " + score);

            if (score > 0) {
                options.put(gram, score);
                sum += score;
            }

            ngram.remove(ngram.size() - 1);
        }

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

    public static void performanceTest(String fileName, int outputLength) {
        NaturalLanguageAdapter nlGenerator = new NaturalLanguageAdapter();

        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();

        long startText = System.currentTimeMillis();
        nlGenerator.addTextFile(fileName);

        System.out.println("Table creation time: " + (System.currentTimeMillis() - startText) + "ms");
        long memoryUsage = ((runtime.totalMemory() - runtime.freeMemory()) - usedMemoryBefore) / 1048576;
        System.out.println("Table size: " + memoryUsage + "MB");

        long startGeneration = System.currentTimeMillis();
        List<String> sentence = new ArrayList<>(Arrays.asList());
        for (int i = 0; i < outputLength; i++) {
            nlGenerator.predictNextGram(sentence);
        }
        long generationTime = System.currentTimeMillis() - startGeneration;
        System.out.println("Generation time: " + generationTime + "ms about " + generationTime / outputLength + "ms per token");
    }

    public static void dictionaryFromCounts(String inDirName, String outFileName, int minOccurrences, int maxSize) throws IOException {
        HashMap<String, Integer> counts = new HashMap();

        Stream<Path> paths = Files.walk(Paths.get(inDirName)).filter(Files::isRegularFile);

        paths.forEach(path -> {
            try (Stream<String> lines = Files.lines(path).filter(s -> !s.isEmpty())) {
                lines.forEach(line -> {
                    String[] split = line.split("\\s");
                    counts.merge(split[0].trim(), Integer.parseInt(split[1].trim()), Integer::sum);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Stream<String> dictionary = counts.entrySet().stream().filter(set -> set.getValue() > minOccurrences)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(maxSize).map(Map.Entry::getKey);;

        Files.write(Paths.get(outFileName), (Iterable<String>) dictionary::iterator);
    }

    public static void main(String[] args) throws IOException {
        //generationTest("/Users/alex/Documents/Marble/Random Text/shakespeare.txt", 200);
        //performanceTest("/Users/alex/Documents/Marble/Random Text/shakespeare.txt", 200);
        dictionaryFromCounts("/Users/alex/Documents/Marble/Random Text/gutenberg/data/counts", "/Users/alex/Documents/Marble/Random Text/gutenberg/data/dictionary.txt", 5, 30000);
    }
}
