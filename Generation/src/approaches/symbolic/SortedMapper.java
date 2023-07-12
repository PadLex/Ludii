package approaches.symbolic;

import main.grammar.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SortedMapper extends SymbolMapper {
    Map<String, Integer> frequency = new HashMap<>();

    @Override
    public List<MappedSymbol> nextPossibilities(Symbol parent, List<? extends Symbol> partialArguments) {
        List<MappedSymbol> nextSymbols = new ArrayList<>(super.nextPossibilities(parent, partialArguments));

        nextSymbols.sort((o1, o2) -> {
            int freq1 = frequency.getOrDefault(o1.toString(), 0);
            int freq2 = frequency.getOrDefault(o2.toString(), 0);
            return Integer.compare(freq2, freq1);
        });

        return nextSymbols;
    }

    public void increment(MappedSymbol symbol) {
        frequency.put(symbol.toString(), frequency.getOrDefault(symbol.toString(), 0) + 1);
    }
}
