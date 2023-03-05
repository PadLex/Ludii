package approaches.random;

import grammar.Grammar;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Symbol;

import java.util.*;
import java.util.stream.IntStream;

public class SymbolMapper {
    private Set<Symbol> symbols = new HashSet<>();
    private Map<String, Set<Symbol>> returnMap = new HashMap<>();
    private Map<Symbol, List<List<Symbol>>> symbolsMap = new HashMap<>();

    public SymbolMapper(Collection<Symbol> symbols) {
        this.symbols.addAll(symbols);

        buildReturnMap();

        // print largest sets in returnMap
        System.out.println("Largest sets in returnMap:");
        returnMap.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue().size())).limit(10)
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue().size()));

        System.out.println("int: " + returnMap.get("int").stream().map(Symbol::token).toList());

        Symbol symbol = Grammar.grammar().findSymbolByPath("game.functions.region.math.Expand");
        List<List<Symbol>> symbolSets = findParameterSets(symbol);
        System.out.println(symbol.info());
        System.out.println(" -> " + symbol.returnType());
        for (List<Symbol> symbolSet : symbolSets) {
            System.out.println(symbolSet);
        }

        System.out.println("obtained from: " + returnMap.get(symbol.path()));

        System.out.println("\n\n\n");

//        buildSymbolMap();
    }

    private void buildReturnMap() {
//        for (Symbol symbol: symbols) {
//            returnMap.put(symbol.path(), new HashSet<>());
//        }
//
//        for (Symbol symbol: symbols) {
//            returnMap.get(symbol.returnType().path()).add(symbol);
//        }

        for (Symbol symbol: symbols) {
            Set<Symbol> returns = returnMap.getOrDefault(symbol.returnType().path(), new HashSet<>());
            returns.add(symbol);
            returnMap.put(symbol.returnType().path(), returns);
        }
    }

    private void buildSymbolMap() {
        for (Symbol symbol: symbols) {
            System.out.println("Mapping " + symbol.path());

            List<List<Symbol>> parameterSets = findParameterSets(symbol);
            parameterSets.sort(Comparator.comparing(List::toString));
            symbolsMap.put(symbol, parameterSets);
        }
    }

    private List<List<Symbol>> findParameterSets(Symbol symbol) {
        List<List<Symbol>> constructorSets = new ArrayList<>();

        if (symbol.isTerminal()) {
            //System.out.println("Symbol " + symbol.name() + " is terminal " + symbol.ludemeType());
            return constructorSets;
        }

        if (symbol.rule() == null) {
            //System.out.println("Symbol " + symbol.name() + " has no rule " + symbol.ludemeType() + " " + symbol.isAbstract());
            return constructorSets;
        }

        for (Clause clause: symbol.rule().rhs()) {
            if (clause.args() == null) {
                continue;
            }
            System.out.println("\n");
//
            System.out.println(clause);
            System.out.println("args:    " + clause.args().stream().map(a -> a.symbol().path()).toList());
//            System.out.println("or:      " + clause.args().stream().map(ClauseArg::orGroup).toList());
//            System.out.println("and:     " + clause.args().stream().map(ClauseArg::andGroup).toList());
//            System.out.println("not opt: " + clause.args().stream().map(arg -> arg.optional()? 0:1).toList());
//
//            System.out.println("man:     " + IntStream.range(0, clause.mandatory().length())
//                    .mapToObj(b -> String.valueOf(clause.mandatory().get(b) ? 1 : 0)).toList());

            // Find flags
            // eg.                    *           *              *
            // orGroup:           [0, 1, 1, 0, 0, 2, 2, 2, 2, 2, 3, 3, 0, 0, 0]
            // argument.optional: [0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
            // optionalFlags ->   [0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1]
            // mandatoryFlags ->  [1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
            BitSet optionalFlags = new BitSet();
            BitSet mandatoryFlags = new BitSet();
            int nextOrGroup = 1;
            int nextAndGroup = 1;
            for (int i = 0; i < clause.args().size(); i++) {
                ClauseArg arg = clause.args().get(i);

                if (arg.orGroup() == nextOrGroup) {
                    optionalFlags.set(i, arg.optional());
                    mandatoryFlags.set(i, !arg.optional());
                    nextOrGroup++;
                } else if (arg.andGroup() == nextAndGroup) {
                    optionalFlags.set(i, arg.optional());
                    mandatoryFlags.set(i, !arg.optional());
                    nextAndGroup++;
                } else if (arg.andGroup() == 0 && arg.orGroup() == 0) {
                    optionalFlags.set(i, arg.optional());
                    mandatoryFlags.set(i, !arg.optional());
                }
            }

//            System.out.println("opt flag: " + IntStream.range(0, optionalFlags.length())
//                    .mapToObj(b -> String.valueOf(optionalFlags.get(b) ? 1 : 0)).toList());
//
//            System.out.println("man flag: " + IntStream.range(0, mandatoryFlags.length())
//                    .mapToObj(b -> String.valueOf(mandatoryFlags.get(b) ? 1 : 0)).toList());

            // Permute optional flags
            // optionalIndexes: [3, 4, 5, 10, 12, 13, 14]
            // possibleSets: [[1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], [1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], ..., [1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1] ]
            List<BitSet> possibleSets = permuteFlags(optionalFlags, mandatoryFlags);

            // Complete selected andGroups
            completeAndGroups(clause.args(), possibleSets);

            // Shift selection in or groups
            int possibleSetsSize = possibleSets.size();
            for (int i = 0; i < possibleSetsSize; i++) {
                recursivelyShiftOrGroups(possibleSets.get(i), 1, clause.args(), possibleSets);
            }

            // check for duplicate sets
//            System.out.println("uniqueness check: " + (possibleSets.size() == possibleSets.stream().distinct().count()));
//
//            System.out.println(possibleSets.size());
//
//            System.out.println("possibleSets: " + possibleSets.stream().map(set -> IntStream.range(0, clause.args().size())
//                    .mapToObj(b -> String.valueOf(set.get(b) ? 1 : 0)).toList()).toList());

            constructorSets.addAll(possibleSets.stream().map(set -> IntStream.range(0, clause.args().size())
                    .mapToObj(b -> set.get(b) ? clause.args().get(b).symbol() : null).toList()).toList());
        }

        // filter for out-of-vocabulary symbols and duplicates
        constructorSets = new ArrayList<>(constructorSets.stream().distinct().filter(l -> symbols.containsAll(l.stream().filter(Objects::nonNull).toList())).toList());

        //System.out.println("constructorSets: " + constructorSets);

        List<List<Symbol>> parameterSets = new ArrayList<>();

        for (List<Symbol> constructorSet : constructorSets) {
            System.out.println("constructorSet: " + constructorSet);
            Set<List<Symbol>> products = cartesianProductOfReturnTypes(constructorSet);
            System.out.println("size: " + products.size());
            parameterSets.addAll(products);
            System.out.println("\n\n----------------------------------------------\n\n ");
        }

        return parameterSets;
    }

    /* Cartesian Product of return types */
    private Set<List<Symbol>> cartesianProductOfReturnTypes(List<Symbol> symbols) {
        return cartesianProductOfReturnTypes(new ArrayList<>(), symbols);
    }
    private Set<List<Symbol>> cartesianProductOfReturnTypes(List<Symbol> traversedSymbols, List<Symbol> remainingSymbols) {
        //System.out.println("traversedSymbols: " + traversedSymbols);
        // Base case
        if (remainingSymbols.isEmpty()) {
            //System.out.println("final: " + traversedSymbols.stream().map(s -> s == null ? "null" : s.path()).toList());
            return Set.of(traversedSymbols);
        }

        Symbol nextSymbol = remainingSymbols.get(0);

        // Skip null symbols
        if (nextSymbol == null) {
            List<Symbol> newlyTraversed = new ArrayList<>(traversedSymbols);
            newlyTraversed.add(null);
            return cartesianProductOfReturnTypes(newlyTraversed, remainingSymbols.subList(1, remainingSymbols.size()));
        }

        // Core recursive cases
        Set<List<Symbol>> parameterSets = new HashSet<>();

        // Eg. game.util.graph.Graph can be obtained from game.util.graph.Graph, game.functions.graph.GraphFunction, game.functions.graph.generators.basis.square.Square, ...
        Set<Symbol> obtainedFrom = returnMap.getOrDefault(nextSymbol.path(), new HashSet<>());
        //System.out.println("obtainedFrom: " + obtainedFrom);

        // Eg. game.functions.graph.GraphFunction can't be obtained, so I'm treating it as its return type, game.util.graph.Graph
        if (nextSymbol.returnType() != nextSymbol) {
            if (Objects.equals(nextSymbol.returnType().path(), nextSymbol.path())) {
                System.out.println("WARNING: " + nextSymbol.path() + " is misbehaving");
            }

            obtainedFrom.addAll(returnMap.getOrDefault(nextSymbol.returnType().path(), Set.of()));
        }

        for (Symbol returnSymbol: obtainedFrom) {

            // TODO trying to filter symbols that can not be initialized
            if (returnSymbol.hidden()) {
                //System.out.println("Skipping: " + returnSymbol.path() + " is hidden");
                continue;
            }

            List<Symbol> newlyTraversed = new ArrayList<>(traversedSymbols);
            newlyTraversed.add(returnSymbol);
            parameterSets.addAll(cartesianProductOfReturnTypes(newlyTraversed, remainingSymbols.subList(1, remainingSymbols.size())));
        }


        return parameterSets;
    }

    private static List<BitSet> permuteFlags(BitSet optionalFlags, BitSet mandatoryFlags) {
        List<BitSet> possibleSets = new ArrayList<>();

        int optionalParams = optionalFlags.cardinality();
        int[] optionalIndexes = IntStream.range(0, optionalFlags.length()).filter(optionalFlags::get).toArray();
//        System.out.println("optionalIndexes: " + Arrays.toString(optionalIndexes));

        int initialPermutations = (int) Math.pow(2, optionalParams);
//        System.out.println("initialPermutations: " + initialPermutations);
        for (int i = 0; i < initialPermutations; i++) {
            BitSet set = (BitSet) mandatoryFlags.clone();
            for (int j = 0; j < optionalIndexes.length; j++) {
                // Decide whether to set the optional parameter based on the jth digit of i in binary form
                set.set(optionalIndexes[j], ((i >> j) & 1) == 1);
            }

            possibleSets.add(set);
        }

        return possibleSets;
    }

    private static void completeAndGroups(List<ClauseArg> clauseArgs, List<BitSet> possibleSets) {
        for (BitSet currentSet: possibleSets) {
            int currentAndGroup = 0;
            boolean active = false;
            for (int j = 0; j < clauseArgs.size(); j++) {
                ClauseArg arg = clauseArgs.get(j);

                if (arg.andGroup() == currentAndGroup && active) {
                    currentSet.set(j);
                }

                if (arg.andGroup() == currentAndGroup + 1) {
                    currentAndGroup++;
                    active = currentSet.get(j);
                }
            }
        }
    }

    private static void recursivelyShiftOrGroups(BitSet currentSet, int currentOrGroup, List<ClauseArg> clauseArgs, List<BitSet> possibleSets) {

        BitSet baseSet = null;
        for (int i = 0; i < clauseArgs.size(); i++) {
            ClauseArg arg = clauseArgs.get(i);
            //System.out.println(arg.orGroup() + " " + currentOrGroup + " " + currentSet.get(i) + " " + baseSet);
            if (arg.orGroup() == currentOrGroup) {
                if (baseSet == null && currentSet.get(i)) {
                    recursivelyShiftOrGroups(currentSet, currentOrGroup + 1, clauseArgs, possibleSets);

                    baseSet = (BitSet) currentSet.clone();
                    baseSet.set(i, false);
                } else if (baseSet != null) {
                    BitSet newSet = (BitSet) baseSet.clone();
                    newSet.set(i);
                    possibleSets.add(newSet);
                    //System.out.println("newSet: " + newSet);

                    recursivelyShiftOrGroups(newSet, currentOrGroup + 1, clauseArgs, possibleSets);
                }
            }
        }

    }

    public static void main(String[] args) {

        List<Symbol> symbols = Grammar.grammar().symbols().stream().filter(Symbol::usedInGrammar).toList();

        new SymbolMapper(symbols);
        System.out.println("done");

        //System.out.println(symbolsMap.get("Sites"));

        /*
        for (Symbol symbol: Grammar.grammar().symbols()) {
            if (symbol.rule() == null) {
                continue;
            }

            for (Clause clause : symbol.rule().rhs()) {
                if (clause.args() == null) {
                    continue;
                }

                for (ClauseArg arg : clause.args()) {
                    if (arg.orGroup() != 0) {
                        System.out.println(symbol.name() + " " + clause);
                    }
                }
            }
        }

         */
    }
}
