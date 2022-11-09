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

	public static enum NumericTokenType {BASE, INT, FLOAT, BOOLEAN, COMPONENT, CONTAINER, STRING, SYMBOL, CLAUSE}
	
	public final float[] floats;
	public final String[] replacementComponents;
	public final String[] containers;
	public final String[] replacementStrings;
	public final int maxPlayers;
	public final Collection<Symbol> symbols;
	
	public static final int openClassToken = 0;				// (
	public static final int closeClassToken = 1;			// )
	public static final int openArrayToken = 2;				// {
	public static final int closeArrayToken = 3;			// }
	public static final int stringedTokensDelimeter = 4;	// " in "1,E,N,W"
	public static final int stringedTokensSeparator = 5;	// , in "20,3,W,N1,E,End"


	public final int baseTokens = 6;
	public final int intTokens;
	public final int floatTokens;
	public final int booleanTokens;
	public final int componentTokens;
	public final int containerTokens;
	public final int stringTokens;

	public Map<String, Integer> symbolToId;
	public Map<Integer, String> idToSymbol;
	public Map<String, Integer> clauseToId;
	public Map<Integer, String> idToClause;

	public final int intStart;
	public final int floatStart;
	public final int booleanStart;
	public final int componentStart;
	public final int containerStart;
	public final int stringStart;
	public final int symbolStart;
	public final int clauseStart;
	public final int tokenCount;
	

	public TokenizationParameters(
			float[] floats,
			String[] replacementComponents,
			String[] containers,
			String[] replacementStrings,
			int maxPlayers,
			Collection<Symbol> symbols,
			int intTokens
	) {
		
		/* initialize parameters */
		Arrays.sort(floats);
		Arrays.sort(containers);
		
		this.floats = floats;
		this.replacementComponents = replacementComponents;
		this.containers = containers;
		this.replacementStrings = replacementStrings;
		this.maxPlayers = maxPlayers;
		this.symbols = Collections.unmodifiableCollection(symbols);
		
		this.intTokens = intTokens;
		this.floatTokens = floats.length;
		this.booleanTokens = 2;
		this.componentTokens = replacementComponents.length * maxPlayers;
		this.containerTokens = containers.length * maxPlayers;
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

		
		/* initialize Starts */
		intStart = baseTokens;
		floatStart = intStart + intTokens;
		booleanStart = floatStart + floatTokens;
		componentStart = booleanStart + booleanTokens;
		containerStart = componentStart + componentTokens;
		stringStart = containerStart + containerTokens;
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

		if (token < componentStart)
			return NumericTokenType.BOOLEAN;
		
		if (token < containerStart)
			return NumericTokenType.COMPONENT;

		if (token < stringStart)
			return NumericTokenType.CONTAINER;

		if (token < symbolStart)
			return NumericTokenType.STRING;

		if (token < clauseStart)
			return NumericTokenType.SYMBOL;

		if (token < tokenCount)
			return NumericTokenType.CLAUSE;

		throw new RuntimeException(token + " token is too large");
	}
	
	
	public static TokenizationParameters completeParameters() {
		float[] floats = {Float.NEGATIVE_INFINITY, -3.5f, -1.5f, -0.5f, -0.325f, 0.0f, 0.17f, 0.25f, 0.4f, 0.5f, 0.5f, 0.6f, 0.65f, 0.707f, 1.05f, 1.333f, 1.4f, 1.5f, 1.73205f, 2.0f, 2.2f, 2.5f, 2.75f, 3.5f, 3.75f, 4.2f, 4.5f, 4.65f, 5.25f, 5.41f, 6.2f, 6.5f, 7.5f, 8.5f, 16.91f, 45.0f, Float.POSITIVE_INFINITY};
		String[] replacementComponents = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King"};
		String[] containers = {"Hand", "Dice", "Deck", "Board"};
		String[] replacementStrings = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "O"};
		int maxPlayers = 14;
		//List<Symbol> completeGrammar = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar()).collect(Collectors.toList());
		List<Symbol> completeGrammar = Grammar.grammar().symbols();

		int intTokens = 600;
		
		return new TokenizationParameters(floats, replacementComponents, containers, replacementStrings, maxPlayers, completeGrammar, intTokens);
	}
}
