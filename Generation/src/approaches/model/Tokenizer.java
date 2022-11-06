package approaches.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import compiler.Compiler;
import game.Game;
import grammar.Grammar;
import graphics.svg.SVGLoader;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Description;
import main.grammar.GrammarRule;
import main.grammar.Symbol;
import main.grammar.Token;
import main.grammar.ebnf.EBNF;
import main.grammar.ebnf.EBNFRule;
import other.GameLoader;


public class Tokenizer {
	
	static final String[] replacementSvg = {"Pawn", "Knite", "Bishop", "Rook", "Queen", "King"};
	static final int maxPlayers = 4;
	
	static final int openClassToken = 0;
	static final int closeClassToken = 1;
	static final int openArrayToken = 2;
	static final int closeArrayToken = 3;

	static final int baseTokens = 4;
	static final int intTokens = 21;
	static final int floatTokens = 21;
	static final int booleanTokens = 2;
	static final int svgTokens = replacementSvg.length * maxPlayers;
	static final int stringTokens = 5;

	private HashMap<String, Integer> symbolToId = new HashMap<>();
	private HashMap<Integer, String> idToSymbol = new HashMap<>();

	private HashMap<String, Integer> clauseToId = new HashMap<>();
	private HashMap<Integer, String> idToClause = new HashMap<>();

	private HashSet<String> svgNames = Arrays.asList(SVGLoader.listSVGs()).stream()
			.map(x -> '"' + x.replaceAll("/.*/", "").replaceAll(".svg", "") +  '"')
			.collect(HashSet::new, HashSet::add, HashSet::addAll);

	public Tokenizer(List<Symbol> symbols) {
		System.out.println(svgNames);
		// TODO sort rules
		for (Symbol symbol : symbols) {
			if (symbol.ludemeType().equals(Symbol.LudemeType.Primitive)) {
				continue;
			}
			// System.out.println(symbol.token());
			// String name = .substring(1, rule.lhs().length()-1).replaceFirst(".+\\.", "");

			// Maybe use different tokens when multiple ludeems share the same alias. Aka
			// try to make ludem to token one-to-one
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
							String clauseLabel = args.label().toLowerCase(); // TODO Why is If uppercase?

							if (!clauseToId.containsKey(clauseLabel)) {
								int id = clauseToId.size();
								clauseToId.put(clauseLabel, id);
								idToClause.put(id, clauseLabel);
								// System.out.println(symbol.token() + ", " + args.label());
							}
						}
					}
				}
				// System.out.println("symbolToken: " + symbol.token() + ", rhs: " +
				// symbol.rule().rhs());
			}

			// System.out.println("symbolToken: " + symbol.token() + ", symbolName: " +
			// symbol.name() + ", rule: "+symbol.rule());

		}
	}
	

	/* Tokenizer functions, LudiiToken to NumericToken */

	public List<Integer> tokenizeGame(Game game) {
		List<Integer> tokens = new ArrayList<>();

		tokenizeTree(game.description().tokenForest().tokenTrees(), tokens, new StringTokenizer(), new SvgTokenizer());

		return tokens;
	}

	private void tokenizeTree(Collection<Token> tree, List<Integer> tokens, StringTokenizer stringTokenizer, SvgTokenizer svgTokenizer) {

		for (Token token : tree) {

			if (token.parameterLabel() != null) {
				// System.out.println(token + ", " + token.parameterLabel());
				tokens.add(clauseStart() + clauseToId.get(token.parameterLabel()));
			}

			if (token.isClass())
				tokens.add(openClassToken);
			else if (token.isArray())
				tokens.add(openArrayToken);

			if (token.name() != null) {
				tokens.add(tokenizeLudiiToken(token, stringTokenizer, svgTokenizer));
			}

			if (!token.isTerminal()) {
				tokenizeTree(token.arguments(), tokens, stringTokenizer, svgTokenizer);
			}

			if (token.isClass())
				tokens.add(closeClassToken);
			else if (token.isArray())
				tokens.add(closeArrayToken);

		}
	}

	private int tokenizeLudiiToken(Token ludiiToken, StringTokenizer stringTokenizer, SvgTokenizer svgTokenizer) {
		// TODO get symbol from token instead of using strings
		String name = ludiiToken.name();

		try {
			return symbolStart() + symbolToId.get(name);
		} catch (NullPointerException ignored) {
		}
		;

		if (name.charAt(0) == '"') {
			try {
				return svgTokenizer.tokenizeSvg(name);
			} catch (RuntimeException ignored) {};
			
			return stringTokenizer.tokenizeString(name);
		}

		try {
			return tokenizeInt(Integer.parseInt(name));
		} catch (NumberFormatException ignored) {};

		try {
			return tokenizeFloat(Float.parseFloat(name));
		} catch (NumberFormatException ignored) {
		}
		;

		if (name.toLowerCase() == "true")
			return tokenizeBoolean(true);
		else if (name.toLowerCase() == "false")
			return tokenizeBoolean(false);

		throw new RuntimeException("Failed to tokenize: " + name);
	}

	// TODO prevent token overflow by returning -1 or raising exception
	private int tokenizeInt(int n) {
		int range = (intTokens - 1) / 2;
		if (n < -range || n > range)
			throw new RuntimeException(n + " int is out of range " + range);

		return intStart() + n + intTokens / 2;
	}

	private int tokenizeFloat(float f) {
		throw new RuntimeException("Not implemented!");
	}

	private int tokenizeBoolean(boolean bool) {
		return booleanStart() + (bool ? 1 : 0);
	}
	
	private class SvgTokenizer {
		private ArrayList<String> previousSvgs = new ArrayList<>();

		private int tokenizeSvg(String string) {
			
			String svgName = string.toLowerCase().replaceAll("\\d","");
			
			if (!svgNames.contains(svgName))
				throw new RuntimeException(svgName + " is not a vald svg name");
			
			int playerIndex = 0;
			try {
				playerIndex = Integer.parseInt(string.replaceAll("[^0-9]", ""));
			} catch (NumberFormatException ignored) {};
			
			if (playerIndex >= maxPlayers) 
				throw new RuntimeException("Too many players " + string);
			
			
			int nameIndex = previousSvgs.indexOf(svgName);

			if (nameIndex < 0) {
				nameIndex = previousSvgs.size();
				previousSvgs.add(svgName);
			}
			
			int finaIndex = nameIndex * maxPlayers + playerIndex;

			if (finaIndex >= svgTokens)
				throw new RuntimeException("Too many unique svgs " + previousSvgs.size());

			return svgStart() + finaIndex;
		}
	}

	private class StringTokenizer {
		private ArrayList<String> previousStrings = new ArrayList<>();

		private int tokenizeString(String string) {
			int id = previousStrings.indexOf(string);

			if (id < 0) {
				id = previousStrings.size();
				previousStrings.add(string);
			}

			if (id >= stringStart() + stringTokens)
				throw new RuntimeException("Too many unique strings " + previousStrings.size());

			return stringStart() + id;
		}
	}
	

	/* Restore functions, NumericToken to LudiiToken */

	public String restoreAsString(List<Integer> tokens) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			str.append(restoreNumericToken(tokens.get(i)));
		}

		return str.toString();
	}

	public Game restoreGame(List<Integer> tokens) {
		try {
			return (Game) Compiler.compileTest(new Description(restoreAsString(tokens)), false);
		} catch (final Exception e) {
		}

		return null;
	}

	private String restoreNumericToken(int token) {
		if (token == openClassToken)
			return "(";

		if (token == closeClassToken)
			return ") ";

		if (token == openArrayToken)
			return "{";

		if (token == closeArrayToken)
			return "} ";

		if (token < floatStart())
			return Integer.toString(restoreInt(token)) + ' ';

		if (token < booleanStart())
			return Float.toString(restoreFloat(token)) + ' ';

		if (token < svgStart())
			return Boolean.toString(restoreBoolean(token)) + ' ';

		if (token < stringStart())
			return restoreSvg(token) + ' ';

		if (token < symbolStart())
			return restoreString(token) + ' ';

		if (token < clauseStart())
			return idToSymbol.get(token - symbolStart()) + ' ';

		if (token < tokenCount())
			return idToClause.get(token - clauseStart()) + ':';

		throw new RuntimeException(token + " token is too large");
	}

	private int restoreInt(int numericToken) {
		return numericToken - intStart() - intTokens / 2;
	}

	private float restoreFloat(int numericToken) {
		throw new RuntimeException("Not implemented!");
	}

	private boolean restoreBoolean(int numericToken) {
		return numericToken - booleanStart() == 1;
	}

	private String restoreSvg(int numericToken) {
		int finalIndex = numericToken - svgStart();
		int nameIndex = finalIndex / maxPlayers;
		int playerIndex = finalIndex % maxPlayers;
				
		if (playerIndex > 0)
			return '"' + replacementSvg[nameIndex] + playerIndex + '"';
		
		return '"' + replacementSvg[nameIndex] + '"';
	}

	private String restoreString(int numericToken) {
		return "\"string " + (numericToken - stringStart()) + "\"";
	}
	

	/* Return first token of a certain type */

	public int intStart() {
		return baseTokens;
	}

	public int floatStart() {
		return intStart() + intTokens;
	}

	public int booleanStart() {
		return floatStart() + floatTokens;
	}

	public int svgStart() {
		return booleanStart() + booleanTokens;
	}

	public int stringStart() {
		return svgStart() + svgTokens;
	}

	public int symbolStart() {
		return stringStart() + stringTokens;
	}

	public int clauseStart() {
		return symbolStart() + symbolToId.size();
	}

	public int tokenCount() {
		return clauseStart() + clauseToId.size();
	}

	
	/* Debugging functions */

	public String dictionary() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < tokenCount(); i++) {
			String str = "null";

			try {
				str = restoreNumericToken(i);
			} catch (RuntimeException ignored) {
			}

			sb.append(str).append(", ");
		}

		return sb.toString();
	}

	public static String squishSpaces(String str) {
		str = str.replaceAll("\\s+", " ");
		str = str.replace(" )", ")");
		str = str.replace(" )", ")");
		str = str.replace(" }", "}");
		str = str.replace("( ", "(");
		str = str.replace("{ ", "{");
		return str;
	}

	public static void main(String[] args) {
		// System.out.println(Grammar.grammar().symbols());
		// System.out.println(Grammar.grammar().aliases());
		// System.out.println(Grammar.grammar().getRules());
		// System.out.println(Grammar.grammar().ludemesUsed());

		// EBNF ebnf = Grammar.grammar().ebnf();
		System.out.println("grammar");

		Tokenizer tokenizer = new Tokenizer(Grammar.grammar().symbols());
		System.out.println("\ntokenizer. Vocabulary size: " + tokenizer.tokenCount() + " tokens");
		System.out.println("Dictionary: " + tokenizer.dictionary());
		System.out.println("caluses: " + tokenizer.clauseToId);

		Game originalGame = GameLoader.loadGameFromFile(new File(
				"/Users/alex/Documents/Marble/Ludii/Common/res/lud/board/war/leaping/diagonal/American Pool Checkers.lud"));
		System.out.println("\ngame loaded");
		System.out.println(squishSpaces(originalGame.description().tokenForest().tokenTrees().get(0).toString()));

		List<Integer> tokens = tokenizer.tokenizeGame(originalGame);
		HashSet<Integer> uniqueTokens = new HashSet<>(tokens);
		System.out.println("\ngame tokenized with " + tokens.size() + " tokens and a vocabulary of "
				+ uniqueTokens.size() + " unique tokens");
		System.out.println(tokens + "\n");

		Tokenizer freshTokenizer = new Tokenizer(Grammar.grammar().symbols());
		String restoredString = freshTokenizer.restoreAsString(tokens);

		System.out.println(squishSpaces(restoredString));

		Game restoredGame = (Game) Compiler.compileTest(new Description(restoredString), false);

		System.out.println("\nrestored game: " + originalGame.description().tokenForest().tokenTrees().toString()
				.equals(restoredGame.description().tokenForest().tokenTrees().toString()));
		System.out.println(squishSpaces(restoredGame.description().tokenForest().tokenTrees().toString()));

		// File out = new File("/Users/alex/Downloads/tokens.json");

	}

}
