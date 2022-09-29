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
import main.grammar.Description;
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
	
	static int argumnetIndexTokens;
	private HashMap<String, Integer> symbolsToId = new HashMap<>();
	private HashMap<Integer, String> idToSymbols = new HashMap<>();

		
	public Tokenizer(List<Symbol> symbols) {
		// TODO sort rules
		for (Symbol symbol: symbols) {
			if (symbol.ludemeType().equals(Symbol.LudemeType.Primitive)) {
				continue;
			}
			//System.out.println(symbol.token());
			//String name = .substring(1, rule.lhs().length()-1).replaceFirst(".+\\.", "");
			
			// Maybe use different tokens when multiple ludeems share the same alias. Aka try to make ludem to token one-to-one
			String symbolName = symbol.token();
			
			if (!symbolsToId.containsKey(symbolName)) {
				int id = symbolsToId.size();
				symbolsToId.put(symbolName, id);
				idToSymbols.put(id, symbol.token());
			}
			
		}
	}
	
	public List<Integer> tokenizeGame(Game game) {
		List<Integer> tokens = new ArrayList<>();
		StringTokenizer stringTokenizer = new StringTokenizer();

		
		tokenizeTree(game.description().tokenForest().tokenTrees(), tokens, stringTokenizer);
		
		return tokens;
	}
	
	private void tokenizeTree(Collection<Token> tree, List<Integer> tokens, StringTokenizer stringTokenizer) {
		//System.out.println("iter: " + tree);
		
		//System.out.print(" < ");
		

		
		for (Token token: tree) {
			
			if (token.isClass()) tokens.add(openClassToken);
			else if (token.isArray()) tokens.add(openArrayToken);
			
			if (token.name() != null) {
				tokens.add(tokenizeGameToken(token, stringTokenizer));
			}
			
			if (!token.isTerminal()) {
				tokenizeTree(token.arguments(), tokens, stringTokenizer);
			}
			
			if (token.isClass()) tokens.add(closeClassToken);
			else if (token.isArray()) tokens.add(closeArrayToken);
			
		}
		
		//System.out.print(" > ");
	}
	
	private int tokenizeGameToken(Token gameToken, StringTokenizer stringTokenizer) {
		// TODO get symbol from token instead of using strings
		String name = gameToken.name();
		int id = symbolsToId.getOrDefault(name, -1);
		
		if (id >= 0) {
			return baseTokens + id;
		}
		
		if (name.charAt(0) == '"') {
			return stringTokenizer.tokenizeString(name);
		}
		
		try {
			return tokenizeInt(Integer.parseInt(name));
		} catch (NumberFormatException ignored) {};
		
		try {
			return tokenizeFloat(Float.parseFloat(name));
		} catch (NumberFormatException ignored) {};
		
		if (name.toLowerCase() == "true") return tokenizeBoolean(true);
		else if (name.toLowerCase() == "false") return tokenizeBoolean(false);
		
		throw new RuntimeException("Failed to tokenize: " + name);
	}
	
	public Game restoreGame(List<Integer> tokens) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			str.append(restoreToken(tokens.get(i)));
			if (tokens.get(i) > baseTokens | (i+1 < tokens.size() && tokens.get(i) != tokens.get(i + 1))) str.append(' ');
		}
		
		System.out.println(str);
		
		try
		{
			return (Game)Compiler.compileTest(new Description(str.toString()), false);
		}
		catch (final Exception e){}
		
		return null;
	}
	
	private String restoreToken(int token) {		
		if (token == openClassToken) return "(";
		if (token == closeClassToken) return ")";
		if (token == openArrayToken) return "{";
		if (token == closeArrayToken) return "}";

		if (token < baseTokens + symbolsToId.size()) return idToSymbols.get(token - baseTokens);
		if (token < baseTokens + symbolsToId.size() + intTokens) return Integer.toString(restoreInt(token));
		if (token < baseTokens + symbolsToId.size() + intTokens + floatTokens) return Float.toString(restoreFloat(token));
		if (token < baseTokens + symbolsToId.size() + intTokens + floatTokens + booleanTokens) return Boolean.toString(restoreBoolean(token));
		if (token < baseTokens + symbolsToId.size() + intTokens + floatTokens + booleanTokens + stringTokens) return restoreString(token);

		throw new RuntimeException(token + " token is too large");
	}

	// TODO prevent token overflow by returning -1 or raising exception
	private int tokenizeInt(int n) {	
		int range = (intTokens-1) / 2;
		if (n < -range || n > range) throw new RuntimeException(n + " int is out of range " + range);
		return baseTokens + symbolsToId.size() + n + intTokens / 2;
	}
	private int restoreInt(int token) {
		return token - (baseTokens + symbolsToId.size()) - intTokens / 2;
	}
	
	private int tokenizeFloat(float f) {
		throw new RuntimeException("Not implemented!");
	}
	private float restoreFloat(int token) {
		throw new RuntimeException("Not implemented!");
	}
	
	private int tokenizeBoolean(boolean bool) {
		return baseTokens + symbolsToId.size() + intTokens + floatTokens + (bool? 1:0);		
	}
	private boolean restoreBoolean(int token) {
		return token - (baseTokens + symbolsToId.size() + intTokens + floatTokens) == 1;
	}
	
	private class StringTokenizer {
		private ArrayList<String> previousStrings = new ArrayList<>();
		
		private int tokenizeString(String string) {
			int id = previousStrings.indexOf(string);
			
			if (id < 0) {
				id = previousStrings.size();
				previousStrings.add(string);
			}
			
			if (id >= baseTokens + symbolsToId.size() + intTokens + floatTokens + booleanTokens + stringTokens) throw new RuntimeException("Too many unique strings " + previousStrings.size());
			
			return baseTokens + symbolsToId.size() + intTokens + floatTokens + booleanTokens + id;
		}
	}
	
	private String restoreString(int token) {
		return "\"string " + (token - (baseTokens + symbolsToId.size() + intTokens + floatTokens + booleanTokens)) + "\"";
	}
	
	public int tokenCount() {
		return baseTokens + symbolsToId.size() + intTokens + floatTokens + booleanTokens + stringTokens;
	}

	public static void main(String[] args) {
		//System.out.println(Grammar.grammar().symbols());
		//System.out.println(Grammar.grammar().aliases());
		//System.out.println(Grammar.grammar().getRules());
		//System.out.println(Grammar.grammar().ludemesUsed());


		//EBNF ebnf = Grammar.grammar().ebnf();
		System.out.println("grammar");

		Tokenizer tokenizer = new Tokenizer(Grammar.grammar().symbols());
		System.out.println("\ntokenizer. Vocabulary size: " + tokenizer.tokenCount() + " tokens");
		System.out.println("Dictionary: " + tokenizer.symbolsToId);

		Game originalGame = GameLoader.loadGameFromFile(new File("/Users/alex/Documents/Ludii/Common/res/lud/board/war/leaping/diagonal/American Pool Checkers.lud"));
		System.out.println("\ngame loaded");
		System.out.println(originalGame.description().tokenForest().tokenTrees().toString().replaceAll("\\s+", " "));


		List<Integer> tokens = tokenizer.tokenizeGame(originalGame);
		HashSet<Integer> uniqueTokens = new HashSet<>(tokens);
		System.out.println("\ngame tokenized with " + tokens.size() + " tokens and a vocabulary of " + uniqueTokens.size() + " unique tokens");
		System.out.println(tokens + "\n");
		
		Tokenizer freshTokenizer = new Tokenizer(Grammar.grammar().symbols()); // Could also use the old one. I'm just making sure that it also works with a fresh one
		Game restoredGame = freshTokenizer.restoreGame(tokens);
		System.out.println("\nrestored game " + restoredGame);

		
		//File out = new File("/Users/alex/Downloads/tokens.json");
	}

}
