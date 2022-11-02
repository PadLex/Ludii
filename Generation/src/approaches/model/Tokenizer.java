package approaches.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import compiler.Compiler;
import game.Game;
import grammar.Grammar;
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
	static final int openClassToken = 0;
	static final int closeClassToken = 1;
	static final int openArrayToken = 2;
	static final int closeArrayToken = 3;

	static final int baseTokens = 4;
	static final int intTokens = 21;
	static final int floatTokens = 21;
	static final int booleanTokens = 2;
	static final int stringTokens = 11;

	private HashMap<String, Integer> symbolToId = new HashMap<>();
	private HashMap<Integer, String> idToSymbol = new HashMap<>();

	private HashMap<String, Integer> clauseToId = new HashMap<>();
	private HashMap<Integer, String> idToClause = new HashMap<>();
	

	public Tokenizer(List<Symbol> symbols) {
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
								//System.out.println(symbol.token() + ", " + args.label());
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
		StringTokenizer stringTokenizer = new StringTokenizer();

		tokenizeTree(game.description().tokenForest().tokenTrees(), tokens, stringTokenizer);

		return tokens;
	}

	private void tokenizeTree(Collection<Token> tree, List<Integer> tokens, StringTokenizer stringTokenizer) {
		// System.out.println("iter: " + tree);

		// System.out.print(" < ");

		for (Token token : tree) {
			
			if (token.parameterLabel() != null) {
				//System.out.println(token + ", " + token.parameterLabel());
				tokens.add(clauseStart() + clauseToId.get(token.parameterLabel()));
			}

			if (token.isClass())
				tokens.add(openClassToken);
			else if (token.isArray())
				tokens.add(openArrayToken);

			if (token.name() != null) {
				tokens.add(tokenizeLudiiToken(token, stringTokenizer));
			}

			if (!token.isTerminal()) {
				tokenizeTree(token.arguments(), tokens, stringTokenizer);
			}

			if (token.isClass())
				tokens.add(closeClassToken);
			else if (token.isArray())
				tokens.add(closeArrayToken);

		}

		// System.out.print(" > ");
	}

	private int tokenizeLudiiToken(Token ludiiToken, StringTokenizer stringTokenizer) {
		// TODO get symbol from token instead of using strings
		String name = ludiiToken.name();

		try {
			return symbolStart() + symbolToId.get(name);
		} catch (NullPointerException ignored) {};

		if (name.charAt(0) == '"') {
			return stringTokenizer.tokenizeString(name);
		}

		try {
			return tokenizeInt(Integer.parseInt(name));
		} catch (NumberFormatException ignored) {};

		try {
			return tokenizeFloat(Float.parseFloat(name));
		} catch (NumberFormatException ignored) {};

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

	private class StringTokenizer {
		private ArrayList<String> previousStrings = new ArrayList<>();

		private int tokenizeString(String string) {
			int id = previousStrings.indexOf(string);

			if (id < 0) {
				id = previousStrings.size();
				previousStrings.add(string);
			}

			if (id >= tokenCount())
				throw new RuntimeException("Too many unique strings " + previousStrings.size());

			return stringStart() + id;
		}
	}
	

	/* Restore functions, NumericToken to LudiiToken */
	
	public Game restoreGame(List<Integer> tokens) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			str.append(restoreNumericToken(tokens.get(i)));
			//if (tokens.get(i) > baseTokens | (i + 1 < tokens.size() && tokens.get(i) != tokens.get(i + 1)))
				//str.append(' ');
		}
		
		// TODO Why do I need "Counter1" ?
		String desc = str.toString();
		desc = desc.replace("string 0", "American Pool Checkers");
		desc = desc.replace("string 1", "Counter");
		desc = desc.replace("string 2", "DoubleCounter");
		desc = desc.replace("string 3", "Counter1");
		desc = desc.replace("string 4", "Counter2");
		desc = squishSpaces(desc);
		System.out.println(desc);


		try {
			return (Game) Compiler.compileTest(new Description(desc), false);
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
		if (token < stringStart())
			return Boolean.toString(restoreBoolean(token)) + ' ';
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

	public int stringStart() {
		return booleanStart() + booleanTokens;
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
		
		for (int i=0; i < tokenCount(); i++) {
			String str = "null";
			
			try {
				str = restoreNumericToken(i);
			} catch(RuntimeException ignored) {}
			
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

		Tokenizer freshTokenizer = new Tokenizer(Grammar.grammar().symbols()); // Could also use the old one. I'm just
																				// making sure that it also works with a
																				// fresh one
		Game restoredGame = freshTokenizer.restoreGame(tokens);
		System.out.println("\nrestored game: " + originalGame.description().tokenForest().tokenTrees().toString().equals(restoredGame.description().tokenForest().tokenTrees().toString()));

		// File out = new File("/Users/alex/Downloads/tokens.json");
		//[0, 80, 1520, 0, 84, 1488, 1, 0, 733, 2, 0, 745, 0, 347, 1494, 1, 1, 0, 37, 1521, 1044, 1099, 1, 0, 37, 1521, 1045, 1107, 1, 0, 37, 1522, 1077, 1, 0, 736, 1044, 0, 574, 1271, 1, 1, 0, 736, 1045, 0, 574, 1270, 1, 1, 3, 1, 0, 141, 0, 121, 2, 0, 116, 1523, 0, 269, 0, 517, 0, 574, 1271, 1, 0, 273, 1489, 1487, 1, 1, 0, 574, 1171, 1487, 1, 1, 1, 0, 116, 1524, 0, 269, 0, 517, 0, 574, 1270, 1, 0, 273, 1489, 1487, 1, 1, 0, 574, 1171, 1487, 1, 1, 1, 3, 1, 0, 266, 0, 134, 0, 389, 1081, 1079, 1, 0, 134, 0, 486, 0, 692, 0, 512, 1293, 1, 1, 0, 623, 1521, 1079, 1, 1, 0, 250, 1206, 0, 38, 0, 512, 1293, 1, 1, 941, 0, 42, 0, 159, 0, 484, 0, 389, 1097, 0, 42, 1, 0, 574, 1280, 1, 1, 1, 0, 389, 949, 0, 700, 0, 42, 1, 1, 1, 1, 0, 194, 0, 168, 0, 42, 1, 1088, 1, 1, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 0, 199, 0, 134, 0, 393, 1222, 0, 250, 1206, 0, 38, 0, 512, 1293, 1, 1, 941, 0, 42, 0, 159, 0, 484, 0, 389, 1097, 0, 42, 1, 0, 574, 1280, 1, 1, 1, 0, 389, 949, 0, 700, 0, 42, 1, 1, 1, 1, 0, 194, 0, 168, 0, 42, 1, 1088, 1, 1, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 1, 1, 0, 182, 1, 0, 134, 0, 389, 1097, 0, 512, 1293, 1, 0, 574, 1080, 1, 1, 0, 220, 0, 512, 1293, 1, 0, 37, 1522, 1, 1079, 1, 1, 1, 1, 1, 0, 205, 1186, 0, 250, 1206, 0, 38, 0, 512, 1293, 1, 1, 941, 0, 42, 0, 72, 978, 1, 0, 72, 978, 1, 0, 159, 0, 484, 0, 389, 1097, 0, 42, 1, 0, 574, 1280, 1, 1, 1, 0, 389, 949, 0, 700, 0, 42, 1, 1, 1, 1, 0, 194, 0, 168, 0, 42, 1, 1088, 1, 1, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 0, 199, 0, 134, 0, 393, 1222, 0, 166, 0, 38, 0, 512, 1293, 1, 1, 941, 0, 42, 0, 72, 978, 1, 0, 72, 978, 1, 0, 159, 0, 484, 0, 389, 1097, 0, 42, 1, 0, 574, 1280, 1, 1, 1, 0, 389, 949, 0, 700, 0, 42, 1, 1, 1, 1, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 1, 1, 0, 182, 1, 1, 1, 1, 1, 1, 0, 209, 2, 0, 162, 0, 94, 1182, 1521, 0, 250, 1206, 0, 38, 1, 941, 0, 42, 0, 159, 0, 484, 0, 389, 1097, 0, 42, 1, 0, 574, 1280, 1, 1, 1, 0, 389, 949, 0, 700, 0, 42, 1, 1, 1, 1, 0, 194, 0, 168, 0, 42, 1, 1088, 1, 1, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 0, 199, 0, 134, 0, 393, 1222, 0, 250, 1206, 0, 38, 0, 512, 1293, 1, 1, 941, 0, 42, 0, 159, 0, 484, 0, 389, 1097, 0, 42, 1, 0, 574, 1280, 1, 1, 1, 0, 389, 949, 0, 700, 0, 42, 1, 1, 1, 1, 0, 194, 0, 168, 0, 42, 1, 1088, 1, 1, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 1, 1, 0, 182, 1, 0, 134, 0, 389, 1097, 0, 512, 1293, 1, 0, 574, 1080, 1, 1, 0, 220, 0, 512, 1293, 1, 0, 37, 1522, 1, 1079, 1, 1, 1, 1, 1, 1, 0, 205, 1186, 0, 94, 1182, 1522, 0, 250, 1206, 941, 0, 42, 0, 72, 978, 1, 0, 72, 978, 1, 0, 389, 949, 0, 700, 0, 42, 1, 1, 1, 0, 194, 0, 168, 0, 42, 1, 1088, 1, 1, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 0, 199, 0, 134, 0, 393, 1222, 0, 166, 0, 38, 0, 512, 1293, 1, 1, 941, 0, 42, 0, 72, 978, 1, 0, 72, 978, 1, 0, 159, 0, 484, 0, 389, 1097, 0, 42, 1, 0, 574, 1280, 1, 1, 1, 0, 389, 949, 0, 700, 0, 42, 1, 1, 1, 1, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 1, 1, 0, 182, 1, 1, 1, 1, 1, 1, 1, 0, 162, 0, 94, 1182, 1521, 0, 250, 1209, 0, 268, 2, 1129, 1123, 3, 1, 0, 39, 0, 389, 945, 0, 39, 1, 1, 1, 1, 0, 199, 0, 134, 0, 389, 1097, 0, 512, 1293, 1, 0, 574, 1080, 1, 1, 0, 220, 0, 512, 1293, 1, 0, 37, 1522, 1, 1079, 1, 1, 1, 1, 0, 94, 1182, 1522, 0, 250, 1203, 941, 1, 1, 1, 3, 1, 1, 1, 0, 140, 0, 134, 0, 123, 1186, 1080, 1, 0, 136, 1079, 1034, 1, 1, 1, 1, 1]
		//( game "string 0" ( players 2 ) ( equipment { ( board ( square 8 )) ( piece "string 1" P1 N ) ( piece "string 1" P2 S ) ( piece "string 2" Each ) ( regions P1 ( sites Bottom )) ( regions P2 ( sites Top )) } ) ( rules ( start { ( place "string 3" ( difference ( expand ( sites Bottom ) ( - 3 1 )) ( sites Phase 1 ))) ( place "string 4" ( difference ( expand ( sites Top ) ( - 3 1 )) ( sites Phase 1 ))) } ) ( play ( if ( is Prev Mover ) ( if ( = ( what ( last To )) ( id "string 1" Mover )) ( move Hop ( from ( last To )) Diagonal ( between ( and ( not ( is In ( between ) ( sites ToClear ))) ( is Enemy ( who ( between )))) ( apply ( remove ( between ) EndOfTurn ))) ( to ( is Empty ( to ))) ( then ( if ( can Move ( move Hop ( from ( last To )) Diagonal ( between ( and ( not ( is In ( between ) ( sites ToClear ))) ( is Enemy ( who ( between )))) ( apply ( remove ( between ) EndOfTurn ))) ( to ( is Empty ( to ))))) ( moveAgain ) ( if ( is In ( last To ) ( sites Next )) ( promote ( last To ) ( piece "string 2" ) Mover ))))) ( max Moves ( move Hop ( from ( last To )) Diagonal ( between ( count Rows ) ( count Rows ) ( and ( not ( is In ( between ) ( sites ToClear ))) ( is Enemy ( who ( between )))) ( apply ( remove ( between ) EndOfTurn ))) ( to ( is Empty ( to ))) ( then ( if ( can Move ( hop ( from ( last To )) Diagonal ( between ( count Rows ) ( count Rows ) ( and ( not ( is In ( between ) ( sites ToClear ))) ( is Enemy ( who ( between ))))) ( to ( is Empty ( to ))))) ( moveAgain )))))) ( priority { ( or ( forEach Piece "string 1" ( move Hop ( from ) Diagonal ( between ( and ( not ( is In ( between ) ( sites ToClear ))) ( is Enemy ( who ( between )))) ( apply ( remove ( between ) EndOfTurn ))) ( to ( is Empty ( to ))) ( then ( if ( can Move ( move Hop ( from ( last To )) Diagonal ( between ( and ( not ( is In ( between ) ( sites ToClear ))) ( is Enemy ( who ( between )))) ( apply ( remove ( between ) EndOfTurn ))) ( to ( is Empty ( to ))))) ( moveAgain ) ( if ( is In ( last To ) ( sites Next )) ( promote ( last To ) ( piece "string 2" ) Mover )))))) ( max Moves ( forEach Piece "string 2" ( move Hop Diagonal ( between ( count Rows ) ( count Rows ) ( is Enemy ( who ( between ))) ( apply ( remove ( between ) EndOfTurn ))) ( to ( is Empty ( to ))) ( then ( if ( can Move ( hop ( from ( last To )) Diagonal ( between ( count Rows ) ( count Rows ) ( and ( not ( is In ( between ) ( sites ToClear ))) ( is Enemy ( who ( between ))))) ( to ( is Empty ( to ))))) ( moveAgain ))))))) ( or ( forEach Piece "string 1" ( move Step ( directions { FR FL } ) ( to ( is Empty ( to )))) ( then ( if ( is In ( last To ) ( sites Next )) ( promote ( last To ) ( piece "string 2" ) Mover )))) ( forEach Piece "string 2" ( move Slide Diagonal ))) } ))) ( end ( if ( no Moves Next ) ( result Mover Win )))))

	}

}
