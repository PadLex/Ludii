package approaches.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import grammar.Grammar;
import graphics.svg.SVGLoader;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Symbol;


public class TokenizationParameters {

	public static enum NumericTokenType {BASE, INT, FLOAT, BOOLEAN, COMPONENT, CONTAINER, STRING, SYMBOL, CLAUSE}
	
	public final double[] numbers;
	public final String[] replacementComponents;
	public final String[] containers;
	public final String[] replacementStrings;
	public final int maxPlayers;
	public final boolean approximateContinuousValiables;    // If false the tokenizer will crash if the exact number is not available. 
															// If true tokenization is lossy since it will numbers as the closes available token.
	
	public static final int incompleteMarker = 0;			// Used in training to demark partial game descriptions. Not used by tokenizer or restorer.
	public static final int openClassToken = 1;				// (
	public static final int closeClassToken = 2;			// )
	public static final int openArrayToken = 3;				// {
	public static final int closeArrayToken = 4;			// }
	public static final int tokenJoiner = 5;				// Join A4 or N1
	public static final int stringedTokensDelimeter = 6;	// " in "1,E,N,W"

	public static final int baseTokens = 7;
	public final int numberTokens;
	public final int booleanTokens;
	public final int componentTokens;
	public final int containerTokens;
	public final int stringTokens;

	public Map<String, Integer> symbolToId;
	public Map<Integer, String> idToSymbol;
	public Map<String, Integer> clauseToId;
	public Map<Integer, String> idToClause;

	public final int numberStart;
	public final int booleanStart;
	public final int componentStart;
	public final int containerStart;
	public final int stringStart;
	public final int symbolStart;
	public final int clauseStart;
	public final int tokenCount;
	

	public TokenizationParameters(
			double[] numbers,
			String[] replacementComponents,
			String[] containers,
			String[] replacementStrings,
			int maxPlayers,
			Collection<Symbol> symbols,
			boolean approximateContinuousValiables
	) {
		
		/* initialize parameters */
		Arrays.sort(numbers);
		Arrays.sort(containers);
		
		this.numbers = numbers;
		this.replacementComponents = replacementComponents;
		this.containers = containers;
		this.replacementStrings = replacementStrings;
		this.maxPlayers = maxPlayers;
		this.approximateContinuousValiables = approximateContinuousValiables;
		
		this.numberTokens = numbers.length;
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
		
		// Add all the letters in the alphabet to support alpha numeric coordinates
		for (char c: "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()) {
			String letter = "" + c;
			if (!symbolToId.containsKey(letter)) {
				int id = symbolToId.size();
				symbolToId.put(letter, id);
				idToSymbol.put(id, letter);
			}
		}
		
		// No idea why this exists
		int id = symbolToId.size();
		symbolToId.put("Undefined", id);
		idToSymbol.put(id, "Undefined");

		
		this.symbolToId = (Map<String, Integer>) Collections.unmodifiableMap(symbolToId);
		this.idToSymbol = (Map<Integer, String>) Collections.unmodifiableMap(idToSymbol);
		this.clauseToId = (Map<String, Integer>) Collections.unmodifiableMap(clauseToId);
		this.idToClause = (Map<Integer, String>) Collections.unmodifiableMap(idToClause);
		
		System.out.println(symbolToId.keySet());
		
		/* initialize Starts */
		numberStart = baseTokens;
		booleanStart = numberStart + numberTokens;
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

		if (token < numberStart)
			return NumericTokenType.BASE;

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
		double[] numbers = {Float.NEGATIVE_INFINITY, -5.6, -5.5, -4.5, -4.45, -3.7, -3.5, -2.9, -2.5, -1.5, -1.31, -1.25, -1, -0.51, -0.5, -0.45, -0.34, -0.325, -0.13, 0.0, 0.1, 0.17, 0.2, 0.25, 0.3, 0.33, 0.38, 0.4, 0.45, 0.5, 0.5, 0.6, 0.625, 0.65, 0.707, 0.74, 0.75, 0.8, 0.88, 0.95, 1.04, 1.05, 1.15, 1.2, 1.3, 1.333, 1.4, 1.5, 1.55, 1.6, 1.73205, 2.0, 2.2, 2.5, 2.75, 2.79, 3.2, 3.25, 3.5, 3.75, 3.9, 4.1, 4.2, 4.3, 4.5, 4.65, 5.25, 5.35, 5.41, 5.5, 6.2, 6.5, 7.5, 8.5, 9.5, 10.44, 11.3, 12.25, 12.66, 14.5, 14.6, 16.91, 45.0, Float.POSITIVE_INFINITY};		
		
		String[] replacementComponents = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King", "Seed", "Counter", "DoubleCounter", "Osho"};
		String[] containers = {"Hand", "Dice", "Deck", "Board"};
		String[] replacementStrings = {"Aaaa", "Bbbb", "Cccc", "Dddd", "Eeee", "Ffff", "Gggg", "Hhhh", "Iiii", "Jjjj", "Kkkk", "Llll", "Oooo", "Mmmm", "Nnnn", "Oooo", "Pppp", "Qqqq", "Rrrr", "Sss", "Tttt", "Uuu",  "Vvvv"};
		int maxPlayers = 20;
		List<Symbol> symbols = SymbolCollections.completeGrammar();
		
		return new TokenizationParameters(numbers, replacementComponents, containers, replacementStrings, maxPlayers, symbols, true);
	}
	
	public static TokenizationParameters boarGameParameters() {
		String[] replacementComponents = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King", "Seed", "Counter", "DoubleCounter", "Osho"};
		String[] containers = {"Hand", "Dice", "Deck", "Board"};
		String[] replacementStrings = {"Aaaa", "Bbbb", "Cccc", "Dddd", "Eeee", "Ffff", "Gggg", "Hhhh", "Iiii", "Jjjj", "Kkkk", "Llll", "Oooo", "Mmmm", "Nnnn", "Oooo", "Pppp", "Qqqq", "Rrrr", "Sss", "Tttt", "Uuu",  "Vvvv"};
		int maxPlayers = 20;
		List<Symbol> symbols = SymbolCollections.completeGrammar();

		
		return new TokenizationParameters(getSparseNumbers(), replacementComponents, containers, replacementStrings, maxPlayers, symbols, false);
	}
	
	static double[] getSparseNumbers() {
		IntStream dense = IntStream.range(-10, 100);
		IntStream sparse = IntStream.range(-5, 20).map(n -> n*100);
		DoubleStream ints = IntStream.concat(dense, sparse).asDoubleStream();
		DoubleStream floats = dense.mapToDouble(n -> n/4.0);
		
		return DoubleStream.concat(ints, floats).distinct().toArray();
	}

}
