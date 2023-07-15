package approaches.symbolic;

import grammar.Grammar;
import main.grammar.Symbol;

import java.util.List;

public class Prediction {
    public static void main(String[] args) {
        SymbolMapper symbolMapper = new SymbolMapper();
        Symbol symbol = Grammar.grammar().findSymbolByPath("game.functions.region.sites.Sites");
        List<Symbol> arguments = List.of();
        System.out.println(symbolMapper.nextPossibilities(symbol, arguments).stream().map(s -> s.path() + "|" + s.nesting()).toList());
    }
}
