package approaches.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import grammar.Grammar;
import graphics.svg.SVGLoader;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Symbol;


public class TokenizationParameters {

	public static enum NumericTokenType {BASE, INT, FLOAT, BOOLEAN, SVG, STRING, SYMBOL, CLAUSE}
	
	public final float[] floats;
	public final String[] replacementSvg;
	public final String[] replacementStrings;
	public final int maxPlayers;
	public final Collection<Symbol> symbols;
	
	public final int openClassToken = 0;
	public final int closeClassToken = 1;
	public final int openArrayToken = 2;
	public final int closeArrayToken = 3;

	public final int baseTokens = 4;
	public final int intTokens;
	public final int floatTokens;
	public final int booleanTokens;
	public final int svgTokens;
	public final int stringTokens;

	public Map<String, Integer> symbolToId;
	public Map<Integer, String> idToSymbol;
	public Map<String, Integer> clauseToId;
	public Map<Integer, String> idToClause;
	public Set<String> svgNames;

	public final int intStart;
	public final int floatStart;
	public final int booleanStart;
	public final int svgStart;
	public final int stringStart;
	public final int symbolStart;
	public final int clauseStart;
	public final int tokenCount;
	

	public TokenizationParameters(
			float[] floats,
			String[] replacementSvg,
			String[] replacementStrings,
			int maxPlayers,
			Collection<Symbol> symbols,
			int intTokens
	) {
		
		/* initialize parameters */
		this.symbols = Collections.unmodifiableCollection(symbols);
		this.replacementSvg = replacementSvg;
		this.replacementStrings = replacementStrings;
		this.maxPlayers = maxPlayers;
		this.floats = floats;
		
		this.intTokens = intTokens;
		this.floatTokens = floats.length;
		this.booleanTokens = 2;
		this.svgTokens = replacementSvg.length * maxPlayers;
		this.stringTokens = replacementStrings.length * maxPlayers;
		
		
		/* initialize symbol and clause Maps */
		HashMap<String, Integer> symbolToId = new HashMap<>();
		HashMap<Integer, String> idToSymbol = new HashMap<>();
		HashMap<String, Integer> clauseToId = new HashMap<>();
		HashMap<Integer, String> idToClause = new HashMap<>();

		for (Symbol symbol : symbols) {
			if (symbol.ludemeType().equals(Symbol.LudemeType.Primitive)) {
				continue;
			}

			String symbolToken = symbol.token();

			if (!symbolToId.containsKey(symbolToken)) {
				int id = symbolToId.size();
				symbolToId.put(symbolToken, id);
				idToSymbol.put(id, symbolToken);
			}
			
			if (symbol.hasAlias()) 
				symbolToId.put(symbol.name().substring(0, 1).toLowerCase() + symbol.name().substring(1), symbolToId.get(symbolToken));
			

			if (symbol.rule() != null) {
				for (Clause clause : symbol.rule().rhs()) {
					if (clause.args() == null)
						continue;

					for (ClauseArg args : clause.args()) {
						if (args.label() != null) {
							String clauseLabel = args.label().substring(0, 1).toLowerCase() + args.label().substring(1); // TODO Why is If capitalized?

							if (!clauseToId.containsKey(clauseLabel)) {
								int id = clauseToId.size();
								clauseToId.put(clauseLabel, id);
								idToClause.put(id, clauseLabel);
							}
						}
					}
				}
			}
		}
		
		this.symbolToId = (Map<String, Integer>) Collections.unmodifiableMap(symbolToId);
		this.idToSymbol = (Map<Integer, String>) Collections.unmodifiableMap(idToSymbol);
		this.clauseToId = (Map<String, Integer>) Collections.unmodifiableMap(clauseToId);
		this.idToClause = (Map<Integer, String>) Collections.unmodifiableMap(idToClause);
		
		System.out.println(symbolToId.keySet());
		
		
		/* initialize svgNames */
		svgNames = Arrays.asList(SVGLoader.listSVGs()).stream()
				.map(x -> '"' + x.replaceAll("/.*/", "").replaceAll(".svg", "").toLowerCase().replaceAll("\\d","") +  '"') // Format from pawn.svg to "pawn"
				.filter((s) -> !s.matches("\\\"[a-z](\\d+)?\\\"") || s.equals("\"\"")) // filter out annoying names like a.svg TODO find better way to selecect svg
				.collect(Collectors.toUnmodifiableSet());
		//System.out.println(svgNames);

		
		/* initialize Starts */
		intStart = baseTokens;
		floatStart = intStart + intTokens;
		booleanStart = floatStart + floatTokens;
		svgStart = booleanStart + booleanTokens;
		stringStart = svgStart + svgTokens;
		symbolStart = stringStart + stringTokens;
		clauseStart = symbolStart + symbolToId.size();
		tokenCount = clauseStart + clauseToId.size();
	}

	public NumericTokenType classifyToken(int token) {
		if (token < 0)
			throw new RuntimeException(token + " token is too samll");

		if (token < intStart)
			return NumericTokenType.BASE;

		if (token < floatStart)
			return NumericTokenType.INT;

		if (token < booleanStart)
			return NumericTokenType.FLOAT;

		if (token < svgStart)
			return NumericTokenType.BOOLEAN;

		if (token < stringStart)
			return NumericTokenType.SVG;

		if (token < symbolStart)
			return NumericTokenType.STRING;

		if (token < clauseStart)
			return NumericTokenType.SYMBOL;

		if (token < tokenCount)
			return NumericTokenType.CLAUSE;

		throw new RuntimeException(token + " token is too large");
	}
	
	
	public static TokenizationParameters completeParameters() {
		float[] floats = {Float.NEGATIVE_INFINITY, -3.5f, -0.5f, 0.25f, 0.5f, 0.5f, 0.65f, 0.707f, 1.05f, 1.333f, 1.5f, 1.73205f, 2.0f, 2.2f, 2.75f, 3.5f, 4.5f, 5.41f, 6.5f, 7.5f, 8.5f, 16.91f, Float.POSITIVE_INFINITY};
		String[] replacementSvg = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King"};
		String[] replacementStrings = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "O"};
		int maxPlayers = 14;
		//List<Symbol> completeGrammar = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar()).collect(Collectors.toList());
		List<Symbol> completeGrammar = Grammar.grammar().symbols();

		int intTokens = 600;
		
		return new TokenizationParameters(floats, replacementSvg, replacementStrings, maxPlayers, completeGrammar, intTokens);
	}
}
