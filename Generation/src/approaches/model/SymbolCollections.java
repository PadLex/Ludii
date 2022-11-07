package approaches.model;

import java.util.List;
import java.util.stream.Collectors;

import grammar.Grammar;
import main.grammar.Symbol;

public class SymbolCollections {
	public static List<Symbol> completeGrammar() {
		return filterHidden(Grammar.grammar().symbols());
	}
	
	public static List<Symbol> reducedGrammar() {
		return completeGrammar();
	}
	
	public static List<Symbol> filterHidden(List<Symbol> symbols) {
		return symbols.stream().filter(s -> s.usedInGrammar()).collect(Collectors.toList());
	}
}
