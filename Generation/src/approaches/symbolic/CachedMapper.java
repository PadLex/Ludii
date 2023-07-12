package approaches.symbolic;

import main.grammar.Symbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachedMapper extends SymbolMapper {
    public Map<String, List<MappedSymbol>> cachedQueries = new HashMap<>();

    @Override
    public List<MappedSymbol> nextPossibilities(Symbol parent, List<? extends Symbol> partialArguments) {
        String key = buildKey(parent, partialArguments);
        List<MappedSymbol> cachedSymbols = cachedQueries.get(key);

        if (cachedSymbols == null) {
            cachedSymbols = super.nextPossibilities(parent, partialArguments);
            cachedQueries.put(key, cachedSymbols);
        }

        return cachedSymbols;
    }

    static String buildKey(Symbol parent, List<? extends Symbol> partialArguments) {
        StringBuilder sb = new StringBuilder();
        sb.append(parent.path()).append('|').append(parent.nesting()).append('(');
        for (Symbol s : partialArguments) {
            sb.append(s.path()).append('|').append(s.nesting()).append(',');
        }
        return sb.append(')').toString();
    }

    /**
     * Saves the cache to a file.
     */
//    public void dump(Path path) {
//        try {
//            Files.write(path,
//                    cachedQueries.entrySet().stream()
//                            .map(entry -> entry.getKey() + "," + entry.getValue())
//                            .collect(Collectors.toList()));
//            System.out.println("Map successfully saved to file: " + fileName);
//        } catch (IOException e) {
//            System.err.println("Error occurred while saving map to file: " + e.getMessage());
//        }
//    }
}
