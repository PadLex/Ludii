package approaches.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import grammar.Grammar;
import main.grammar.Symbol;

public class SymbolCollections {
	
	static HashSet<String> boardGameSymbolNames = new HashSet<>(Arrays.asList("Visited", "Left", "tiling", "wedge", "Repeat", "moveAgain", "Liberties", "score", "path", "Pass", "firstMoveOnTrack", "centrePoint", "pass", "%", "Hop", "*", "+", "-", "/", "Line", "Related", "size", "EndOfTurn", "<", "!=", "=", "Blocked", ">", "A", "handSite", "B", "C", "Set", "D", "E", "ahead", "F", "G", "H", "hop", "roll", "renumber", "I", "J", "BL", "K", "L", "Hexagonal", "trackSite", "M", "N", "O", "result", "P", "BR", "Q", "R", "S", "dual", "T", "Propose", "U", "V", "W", "X", "Y", "MoveLimit", "Z", "flip", "^", "hand", "Players", "WNW", "Die", "set", "column", "spiral", "Columns", "union", "Bet", "abs", "CW", "flips", "Forward", "Add", "Backwards", "Neutral", "Start", "Around", "sites", "do", "Enemy", "intersection", "Undefined", "count", "ToClear", "avoidStoredState", "Hand", "graph", "piece", "State", "allCombinations", "Bottom", "End", "Corners", "ENE", "FromBottom", "DiceUsed", "FL", "Outer", "FR", "UNE", "remove", "end", "Radiating", "UNW", "Diamond", "counter", "LastTo", "hole", "Triggered", "mover", "WSW", "append", "regionSite", "Remove", "Odd", "Different", "Board", "Hidden", "id", "Even", "if", "Distance", "Sides", "Pieces", "is", "Square", "Sites", "boardless", "surakartaBoard", "Top", "Alternating", "rotations", "skew", "Vertex", "DiceEqual", "Draw", "In", "Right", "Direction", "mapEntry", "Off", "Side", "SameLayer", "Slide", "ESE", "directional", "MovesThisTurn", "Threatened", "T33434", "surround", "map", "Orthogonal", "max", "USE", "trigger", "Leftward", "Playable", "USW", "All", "pathExtent", "rotate", "pips", "while", "Concentric", "Vertices", "Counter", "Random", "Swap", "claim", "sow", "Team2", "Team1", "Cell", "all", "Player", "Occupied", "level", "swap", "Between", "equipment", "tri", "arrayValue", "expand", "forget", "directions", "NE", "Value", "Row", "TeamMover", "no", "Phase", "NW", "EndSite", "and", "repeat", "row", "Rows", "Adjacent", "P1", "P2", "toInt", "Hexagon", "or", "P3", "P4", "SSE", "P5", "forEach", "custodial", "P6", "P7", "P8", "Centre", "Array", "Diagonal", "SSW", "Pips", "Incident", "nextPhase", "Edge", "Track", "Each", "Connected", "shift", "prev", "Inner", "Farthest", "sizes", "merge", "state", "SidesMatch", "AnyDie", "mancalaBoard", "then", "Rotational", "Pot", "meta", "NextPlayer", "difference", "NotEmpty", "Group", "PositionalInTurn", "Solid", "remember", "promote", "SE", "min", "pot", "Star", "Piece", "Backward", "Groups", "Limping", "where", "Team", "SW", "face", "LineOfSight", "Prev", "topLevel", "Select", "makeFaces", "to", "intervene", "Passed", "NonMover", "NNE", "rules", "Full", "NNW", "tile", "To", "results", "Territory", "last", "FirstSite", "Stack", "SameDirection", "Score", "next", "Forwards", "attract", "Step", "What", "Mover", "Empty", "not", "Moves", "concentric", "vote", "enclose", "FromTop", "Loop", "Perimeter", "apply", "start", "was", "Rotation", "T3636", "pair", "Captures", "what", "OppositeDirection", "Within", "Decided", "step", "play", "Var", "LargePiece", "Rectangle", "Triangle", "Count", "Shared", "hex", "between", "phase", "Vote", "players", "byScore", "var", "Proposed", "priority", "Cycle", "push", "Out", "slide", "Next", "exact", "Level", "Shoot", "Site", "regions", "NoEnd", "range", "fromTo", "layer", "coord", "Friend", "leap", "place", "regular", "Pattern", "Loss", "Own", "Promote", "site", "None", "board", "<=", "Rightward", "T3464", "dice", "from", "LevelTo", "Who", "Turns", "SidesNoCorners", "Leap", "square", "Active", "keep", "Flat", "rectangle", "Win", "Prism", "who", ">=", "Steps", "LastSite", "RememberValue", "note", "game", "passEnd", "values", "scale", "automove", "can", "Move", "Column", "array", "addScore", "splitCrossings", "Remembered", "track", "value", "player", "move", "amount", "TurnLimit", "From", "CCW", "poly", "Pending"));
	
	public static List<Symbol> completeGrammar() {
		return filterHidden(Grammar.grammar().symbols());
	}
	
	public static List<Symbol> boardGames() {
		return filterByName(Grammar.grammar().symbols(), boardGameSymbolNames);
	}
	
	public static List<Symbol> filterHidden(List<Symbol> symbols) {
		return symbols.stream().filter(s -> s.usedInGrammar()).collect(Collectors.toList());
	}
	
	public static List<Symbol> filterByName(List<Symbol> symbols, HashSet<String> boardGameSymbolNames2) {
		return symbols.stream().filter(s -> boardGameSymbolNames.contains(s.name())).collect(Collectors.toList());
	}
	
	
}
