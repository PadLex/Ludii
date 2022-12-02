package approaches.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

	public static enum NumericTokenType {BASE, INT, DECIMAL, BOOLEAN, COMPONENT, CONTAINER, STRING, SYMBOL, CLAUSE}
	
	public final int[] ints;
	public final double[] decimals;
	public final String[] replacementComponents;
	public final String[] containers;
	public final String[] replacementStrings;
	public final int maxPlayers;
	public final boolean approximateContinuousValiables;    // If false the tokenizer will crash if the exact number is not available. 
															// If true tokenization is lossy since it will numbers as the closes available token.
	public static final double floatPrecision = 0.0001;
	
	public static final int incompleteMarker = 0;			// Used in training to demark partial game descriptions. Not used by tokenizer or restorer.
	public static final int openClassToken = 1;				// (
	public static final int closeClassToken = 2;			// )
	public static final int openArrayToken = 3;				// {
	public static final int closeArrayToken = 4;			// }
	public static final int coordinateJoiner = 5;			// Join A4 or N1
	public static final int stringedTokensDelimeter = 6;	// " in "1,E,N,W"
	public static final int floatJoiner = 7;				//  Join 1 and 0.78 to form 1.78

	public static final int baseTokens = 8;
	public final int intTokens;
	public final int decimalTokens;
	public final int booleanTokens;
	public final int componentTokens;
	public final int containerTokens;
	public final int stringTokens;

	public Map<String, Integer> symbolToId;
	public Map<Integer, String> idToSymbol;
	public Map<String, Integer> clauseToId;
	public Map<Integer, String> idToClause;

	public final int intStart;
	public final int decimalStart;
	public final int booleanStart;
	public final int componentStart;
	public final int containerStart;
	public final int stringStart;
	public final int symbolStart;
	public final int clauseStart;
	public final int tokenCount;
	

	public TokenizationParameters(
			int[] ints,
			double decimals[],
			String[] replacementComponents,
			String[] containers,
			String[] replacementStrings,
			int maxPlayers,
			Collection<Symbol> symbols,
			boolean approximateContinuousValiables
	) {
		
		/* initialize parameters */
		Arrays.sort(ints);
		Arrays.sort(decimals);
		Arrays.sort(containers);
		
		this.ints = ints;
		this.decimals = decimals;
		this.replacementComponents = replacementComponents;
		this.containers = containers;
		this.replacementStrings = replacementStrings;
		this.maxPlayers = maxPlayers;
		this.approximateContinuousValiables = approximateContinuousValiables;
		
		this.intTokens = ints.length;
		this.decimalTokens = decimals.length;
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
		
		id = symbolToId.size();
		symbolToId.put("Infinity", id);
		idToSymbol.put(id, "Infinity");

		
		this.symbolToId = (Map<String, Integer>) Collections.unmodifiableMap(symbolToId);
		this.idToSymbol = (Map<Integer, String>) Collections.unmodifiableMap(idToSymbol);
		this.clauseToId = (Map<String, Integer>) Collections.unmodifiableMap(clauseToId);
		this.idToClause = (Map<Integer, String>) Collections.unmodifiableMap(idToClause);
		
		System.out.println(symbolToId.keySet());
		
		/* initialize Starts */
		intStart = baseTokens;
		decimalStart = intStart + intTokens;
		booleanStart = decimalStart + decimalTokens;
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
		
		if (token < decimalStart)
			return NumericTokenType.INT;

		if (token < booleanStart)
			return NumericTokenType.DECIMAL;

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
	
	public int getFloat(double[] a, int fromIndex, int toIndex, double key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = a[mid];

            if (midVal < key)
                low = mid + 1;  // Neither val is NaN, thisVal is smaller
            else if (midVal > key)
                high = mid - 1; // Neither val is NaN, thisVal is larger
            else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(key);
                if (midBits == keyBits)     // Values are equal
                    return mid;             // Key found
                else if (midBits < keyBits) // (-0.0, 0.0) or (!NaN, NaN)
                    low = mid + 1;
                else                        // (0.0, -0.0) or (NaN, !NaN)
                    high = mid - 1;
            }
        }
        return -(low + 1);  // key not found.
    }
	
	
	public static TokenizationParameters completeParameters() {		
		String[] replacementComponents = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King", "Seed", "Counter", "DoubleCounter", "Osho", "RightChariot", "BlindMonkey", "CatSword", "ChineseCock", "Donkey", "BlindTiger", "FreeDragon", "OldKite", "NorthernBarbarian", "GoBetween", "HornedFalcon", "BuddhistSpirit", "Peacock", "FlyingDragon", "SouthernBarbarian", "Lion", "TurtleSnake", "WaterBuffalo", "FreeCopper", "RecliningDragon", "FreeSerpent", "StoneGeneral", "BlueDragon", "IronGeneral", "Deva", "PoisonSnake", "FreeGold", "GoldenDeer", "FreeDreamEater", "RushingBird", "FreeEarth", "EvilWolf", "FreeBear", "Bishop", "SideDragon", "RamsHeadSoldier", "EasternBarbarian", "FragrantElephant", "AngryBoar", "BuddhistDevil", "FreeIron", "FuriousFiend", "Soldier", "CopperGeneral", "LeftGeneral", "FreeGoer", "TileGeneral", "FreeSilver", "FreeTiger", "OldRat", "FreeTile", "EarthGeneral", "WhiteElephant", "SilverHare", "GuardianOfTheGods", "OldMonkey", "LongNosedGoblin", "FlyingHorse", "ViolentOx", "Wrestler", "HookMover", "Kirin", "EnchantedBadger", "Pawn", "Emperor", "FreeCat", "Prince", "StandardBearer", "VerticalMover", "Disc", "GreatElephant", "Knight", "TeachingKing", "SheDevil", "SideChariot", "DrunkenElephant", "SilverGeneral", "LeftChariot", "WhiteHorse", "GreatDragon", "WizardStork", "Queen", "FreeDemon", "WhiteTiger", "ReverseChariot", "LionDog", "FierceEagle", "Dove", "NeighborKing", "HowlingDog", "WesternBarbarian", "GoldGeneral", "DragonHorse", "Lance", "DarkSpirit", "VermillionSparrow", "FreeStone", "FreeBoar", "RightGeneral", "SideMover", "FlyingOx", "FreeWolf", "Phoenix", "FreeLeopard", "Rook", "DragonKing", "Capricorn", "ViolentBear", "CoiledSerpent", "MountainWitch", "BlindBear", "Whale", "Bat", "PrancingStag", "SoaringEagle", "GoldenBird", "SquareMover", "FerociousLeopard", "WoodGeneral"};
		String[] containers = {"Hand", "Dice", "Deck", "Board"};
		String[] replacementStrings = {"Aaaa", "Bbbb", "Cccc", "Dddd", "Eeee", "Ffff", "Gggg", "Hhhh", "Iiii", "Jjjj", "Kkkk", "Llll", "Oooo", "Mmmm", "Nnnn", "Oooo", "Pppp", "Qqqq", "Rrrr", "Sss", "Tttt", "Uuu",  "Vvvv"};
		int maxPlayers = 20;
		List<Symbol> symbols = SymbolCollections.completeGrammar();
		int[] ints = IntStream.range(-2000, 3000).toArray();
		double[] decimals = calculateDecimals(1000);
		System.out.print(Arrays.toString(decimals));
				
		return new TokenizationParameters(ints, decimals, replacementComponents, containers, replacementStrings, maxPlayers, symbols, false);
	}
	
	public static TokenizationParameters boarGameParameters() {
		String[] replacementComponents = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King", "Seed", "Counter", "DoubleCounter"};
		String[] containers = {"Hand", "Dice", "Deck", "Board"};
		String[] replacementStrings = {"Aaaa", "Bbbb", "Cccc", "Dddd", "Eeee", "Ffff", "Gggg", "Hhhh", "Iiii", "Jjjj", "Kkkk", "Llll", "Oooo", "Mmmm", "Nnnn", "Oooo", "Pppp", "Qqqq", "Rrrr", "Sss", "Tttt", "Uuu",  "Vvvv"};
		int maxPlayers = 20;
		List<Symbol> symbols = SymbolCollections.completeGrammar();
		int[] ints = IntStream.concat(IntStream.range(-10, 100), IntStream.range(-5, 20).map(n -> n*100)).distinct().toArray();
		double[] decimals = calculateDecimals(10);
		
		return new TokenizationParameters(ints, decimals, replacementComponents, containers, replacementStrings, maxPlayers, symbols, true);
	}
	
	static double[] calculateDecimals(int fraction) {
		return IntStream.range(0, fraction).mapToDouble(n -> n/((double) fraction)).toArray();
	}
}
