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

    final static int maxSize = 65536;

    String startGram = "<s>";
    String endGram = "</s>";

    String outOfDictionaryGram = "<?>";

    FrequencyTable frequencyTable = new SimpleTrie(6);

    HashMap<String, Short> dictionary;

    Random random = new Random();

    NaturalLanguageAdapter(HashMap<String, Short> dictionary) {
        this.dictionary = dictionary;
    }

    public void incrementTokens(List<String> gramSequence) {
        gramSequence = new ArrayList<>(gramSequence);

        for (int i = 0; i < frequencyTable.maxN; i++) {
            gramSequence.add(0, startGram);
        }
        gramSequence.add(endGram);

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
        for (Map.Entry<String, Short> gram: dictionary.entrySet()) {
            ngram.add(gram.getKey());

            double score = frequencyTable.stupidBackoffScore(ngram, 0.8);

            if (Double.isInfinite(score) || Double.isNaN(score) || score < 0)
                throw new Error("Double underflow, plz fix: " + ngram + " -> " + score);

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
        Stream<Path> paths = Files.walk(Paths.get(tokenDirectory)).filter(Files::isRegularFile).limit(50);

        paths.forEach(path -> {
            try {
                List<String> lines = Files.readAllLines(path).stream().map(String::strip).filter(s -> !s.isEmpty()).map(s -> dictionary.containsKey(s)? s : outOfDictionaryGram).toList();
                if (lines.stream().filter(s -> s.equals(outOfDictionaryGram)).count() / (double) lines.size() > 0.05) {
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

        Stream<String> stream = Files.lines(Paths.get(dictionaryFile)).limit(maxSize);
        stream.map(String::strip).filter(s -> !s.isEmpty()).sequential().forEach(s -> dictionary.put(s, (short) (dictionary.size() - maxSize/2)));

        return dictionary;
    }
    public static void generationTest(String dataDirectory, int outputLength) throws IOException {

        NaturalLanguageAdapter nlGenerator = new NaturalLanguageAdapter(loadDictionary(dataDirectory + "/dictionary.txt"));

        nlGenerator.addTokenFiles(dataDirectory + "/tokens/");

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
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(maxSize).map(Map.Entry::getKey);;

        Files.write(Paths.get(dataDirectory + "/dictionary.txt"), (Iterable<String>) dictionary::iterator);
    }

    public static void main(String[] args) throws IOException {
        //generationTest("/Users/alex/Documents/Marble/Random Text/gutenberg/data", 200);
        performanceTest("/Users/alex/Documents/Marble/Random Text/gutenberg/data", 200);
        //dictionaryFromCounts("/Users/alex/Documents/Marble/Random Text/gutenberg/data", 5);
    }
}
