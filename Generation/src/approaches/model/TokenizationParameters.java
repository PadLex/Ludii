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
			int maxPlayers,
			Collection<Symbol> symbols,
			int intTokens,
			int stringTokens
	) {
		
		/* initialize parameters */
		this.symbols = Collections.unmodifiableCollection(symbols);
		this.replacementSvg = replacementSvg;
		this.maxPlayers = maxPlayers;
		this.floats = floats;
		
		this.intTokens = intTokens;
		this.floatTokens = floats.length;
		this.booleanTokens = 2;
		this.svgTokens = replacementSvg.length * maxPlayers;
		this.stringTokens = stringTokens;
		
		
		/* initialize symbol and clause Maps */
		HashMap<String, Integer> symbolToId = new HashMap<>();
		HashMap<Integer, String> idToSymbol = new HashMap<>();
		HashMap<String, Integer> clauseToId = new HashMap<>();
		HashMap<Integer, String> idToClause = new HashMap<>();

		for (Symbol symbol : symbols) {
			if (symbol.ludemeType().equals(Symbol.LudemeType.Primitive)) {
				continue;
			}

			String symbolName = symbol.token();

			if (!symbolToId.containsKey(symbolName)) {
				int id = symbolToId.size();
				symbolToId.put(symbolName, id);
				idToSymbol.put(id, symbolName);
			}

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
		
		
		/* initialize svgNames */
		svgNames = Arrays.asList(SVGLoader.listSVGs()).stream().map(x -> '"' + x.replaceAll("/.*/", "").replaceAll(".svg", "") +  '"').collect(Collectors.toUnmodifiableSet());
		

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
		float[] floats = {0.5f, 0.25f, 1.333f, 3.5f, 2.75f};
		String[] replacementSvg = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King"};
		int maxPlayers = 16;
		//List<Symbol> completeGrammar = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar()).collect(Collectors.toList());
		List<Symbol> completeGrammar = Grammar.grammar().symbols();

		
		int intTokens = 100;
		int stringTokens = 40;
		
		return new TokenizationParameters(floats, replacementSvg, maxPlayers, completeGrammar, intTokens, stringTokens);
	}
}
