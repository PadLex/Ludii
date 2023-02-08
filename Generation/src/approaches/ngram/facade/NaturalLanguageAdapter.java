package approaches.ngram.facade;

import approaches.ngram.table.FrequencyTable;
import approaches.ngram.table.HashTableTrie;
import approaches.ngram.table.ListTrie;
import approaches.ngram.table.SimpleHashTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class NaturalLanguageAdapter {

    final static short startGram = (short) 32768;
    final static short endGram = (short) 32767;
    final static short outOfDictionaryGram = (short) 32766;
    final static int maxDictionarySize = 32765 * 2;

    FrequencyTable frequencyTable = new ListTrie(4);

    HashMap<String, Short> dictionary;

    Random random = new Random(0);

    NaturalLanguageAdapter(HashMap<String, Short> dictionary) {
        this.dictionary = dictionary;
    }

    public void incrementTokens(List<String> gramStringSequence) {
        List<Short> gramSequence = new ArrayList<>(gramStringSequence.stream().map(s -> dictionary.getOrDefault(s, outOfDictionaryGram)).toList());

        for (int i = 0; i < frequencyTable.maxN; i++) {
            gramSequence.add(0, startGram);
        }
        gramSequence.add(endGram);

        for (int i = 0; i <= gramSequence.size() - frequencyTable.maxN; i++) {
            frequencyTable.incrementAll(gramSequence.subList(i, i + frequencyTable.maxN));
        }
    }

    public String predictNextGram(List<String> gramStringSequence) {
        List<Short> ngram = new ArrayList<>(gramStringSequence.stream().map(s -> dictionary.getOrDefault(s, outOfDictionaryGram)).toList());

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
        for (Map.Entry<String, Short> gram: dictionary.entrySet()) {
            ngram.add(gram.getValue());

            double score = frequencyTable.stupidBackoffScore(ngram, 0.4);

            if (Double.isInfinite(score) || Double.isNaN(score) || score < 0)
                continue;
                //throw new Error("Double underflow, plz fix: " + ngram + " -> " + score);

            if (score > 0) {
                options.put(gram.getKey(), score);
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
            stream.map(String::strip).filter(s -> !s.isEmpty()).map(s -> Arrays.asList(s.split(" "))).forEach(this::incrementTokens);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addTokenFiles(String tokenDirectory) throws IOException {
        Stream<Path> paths = Files.walk(Paths.get(tokenDirectory)).filter(Files::isRegularFile).limit(1000);

        paths.forEach(path -> {
            try {
                List<String> lines = Files.readAllLines(path).stream().map(String::strip).filter(s -> !s.isEmpty()).toList();

                if (lines.stream().filter(s -> !dictionary.containsKey(s)).count() / (double) lines.size() > 0.05) {
                    System.out.println("out of dict: " + path);
                    return;
                }


                incrementTokens(lines);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static HashMap<String, Short> loadDictionary(String dictionaryFile) throws IOException {
        HashMap<String, Short> dictionary = new HashMap<>();

        Stream<String> stream = Files.lines(Paths.get(dictionaryFile)).limit(maxDictionarySize);
        stream.map(String::strip).filter(s -> !s.isEmpty()).sequential().forEach(s -> dictionary.put(s, (short) (dictionary.size() - maxDictionarySize /2)));

        return dictionary;
    }
    public static void generationTest(String dataDirectory, int outputLength) throws IOException {

        NaturalLanguageAdapter nlGenerator = new NaturalLanguageAdapter(loadDictionary(dataDirectory + "/dictionary.txt"));

        nlGenerator.addTokenFiles(dataDirectory + "/tokens/");

        //System.out.println(nlGenerator.frequencyTable);

        String[] sentences = {"", "i ", "i will run to the ", "a car", "mit figuren erdteile länderkunde", "io voglio"};
        for (String sentenceStrings: sentences) {
            List<String> sentence = new ArrayList(Arrays.asList(sentenceStrings.split("\\s")));
            System.out.print("\n" + sentence + " + ");
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
    }

    public static void performanceTest(String dataDirectory, int outputLength) throws IOException {
        NaturalLanguageAdapter nlGenerator = new NaturalLanguageAdapter(loadDictionary(dataDirectory + "/dictionary.txt"));

        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        long startText = System.currentTimeMillis();
        nlGenerator.addTokenFiles(dataDirectory + "/tokens/");

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

    public static void dictionaryFromCounts(String dataDirectory, int minOccurrences) throws IOException {
        HashMap<String, Integer> counts = new HashMap();

        Stream<Path> paths = Files.walk(Paths.get(dataDirectory + "/counts/")).filter(Files::isRegularFile);

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
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(maxDictionarySize).map(Map.Entry::getKey);;

        Files.write(Paths.get(dataDirectory + "/dictionary.txt"), (Iterable<String>) dictionary::iterator);
    }

    public static void main(String[] args) throws IOException {
        //generationTest("/Users/alex/Documents/Marble/Random Text/gutenberg/data", 10);
        performanceTest("/Users/alex/Documents/Marble/Random Text/gutenberg/data", 20);
        //dictionaryFromCounts("/Users/alex/Documents/Marble/Random Text/gutenberg/data", 5);
    }
}
