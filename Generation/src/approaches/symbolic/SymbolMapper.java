package approaches.symbolic;

import grammar.Grammar;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Symbol;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SymbolMapper {
    private Set<Symbol> symbols = new HashSet<>();
    private Set<String> paths = new HashSet<>();
    private Map<String, List<Symbol>> compatibilityMap = new HashMap<>();

    // Maps symbols to every possible set of base-symbols (aka parameters) that can be used to initialize them.
    // eg game.util.graph.Graph can be initialized using [<Float>, null], [<Float>, <Integer>], [], or [<graph>]
    private Map<String, List<List<ClauseArg>>> parameterMap = new HashMap<>();

    // To obtain every possible set of symbols which can be used to initialize another symbol, you would need replace
    // each base-symbol with it's corresponding source symbols and take their cartesian product. Unfortunately,
    // this is too intensive to pre-compute.

    public SymbolMapper(Collection<Symbol> symbols) {
        this.symbols.addAll(symbols);
        this.paths.addAll(symbols.stream().map(Symbol::path).toList());

        buildSymbolMap();
        buildCompatibilityMap();
//        Symbol symbol = Grammar.grammar().findSymbolByPath("game.util.graph.Graph");
//        List<List<Symbol>> symbolSets = findParameterSets(symbol);
//        System.out.println(symbol.info());
//        System.out.println(" -> " + symbol.returnType());
//        System.out.println("obtained from: " + sourceMap.get(symbol.path()));
//        System.out.println("can be initialized with: ");
//        symbolSets.forEach(System.out::println);
    }

    public List<Symbol> nextPossibilities(Symbol parent, List<Symbol> partialArguments) {
        //System.out.println("Symbol mapper: " + parent.path() + " -> " + parameterMap.get(parent.path()));
        //sourceMap.get(parent.path()).stream().mapMulti((source, consumer) -> parameterMap.get(source.path()).forEach(consumer))
        Stream<List<ClauseArg>> parameterSets = parameterMap.get(parent.path()).stream();

        parameterSets = parameterSets.filter(completeArguments -> {
            if (partialArguments.size() >= completeArguments.size())
                return false;

            for (int i = 0; i < partialArguments.size(); i++) {

                if (completeArguments.get(i) == null ^ partialArguments.get(i) == null)
                    return false;

                if (completeArguments.get(i) == null && partialArguments.get(i) == null)
                    continue;

                if (completeArguments.get(i).symbol().compatibleWith(partialArguments.get(i)))
                    continue;

//                if (partialArguments.get(i).compatibleWith(completeArguments.get(i).symbol())) {
//                    continue;
//                }
//
//                if (partialArguments.get(i).validReturnType(completeArguments.get(i))) {
//                    continue;
//                }

                return false;
            }

            return true;
        });


        Map<String, Symbol> possibilities = new HashMap<>();
        parameterSets.forEach(args -> {
            ClauseArg arg = args.get(partialArguments.size());
            if (arg == null) {
                possibilities.put("null", null);
                return;
            }

            for (Symbol symbol: compatibilityMap.get(arg.symbol().path())) {
                symbol = new Symbol(symbol);
                symbol.setNesting(arg.nesting());
                possibilities.put(symbol.path() + "|" + symbol.nesting(), symbol);
            }

        });

        return possibilities.values().stream().sorted(Comparator.comparing(s -> s!=null? s.path():"")).toList();
    }
    private void buildCompatibilityMap() {
        for (Symbol symbol: symbols) {
            compatibilityMap.put(symbol.path(), new ArrayList<>());
        }

        for (Symbol symbol: symbols) {
            for (Symbol other: symbols) {
                if (symbol.compatibleWith(other)) {
                    compatibilityMap.get(symbol.path()).add(other);
                }
            }
        }
    }

    private void buildSymbolMap() {
        for (Symbol symbol: symbols) {
            List<List<ClauseArg>> parameterSets = new ArrayList<>(findParameterSets(symbol));
            parameterSets.sort(Comparator.comparing(List::toString));
            parameterMap.put(symbol.path(), parameterSets);
        }
    }

    private List<List<ClauseArg>> findParameterSets(Symbol symbol) {
        List<List<ClauseArg>> constructorSets = new ArrayList<>();

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
//            System.out.println("\n");
//
//            System.out.println(clause);
//            System.out.println("args:    " + clause.args().stream().map(a -> a.symbol().path()).toList());
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
                    .mapToObj(i -> set.get(i)? clause.args().get(i) : null).toList()).toList());
        }

        // filter for out-of-vocabulary symbols and duplicates
        Stream<List<ClauseArg>> parameterStream = constructorSets.stream().distinct();
        parameterStream = parameterStream.filter(l -> paths.containsAll(l.stream().filter(Objects::nonNull).map(ClauseArg::symbol).map(Symbol::path).toList()));

        return parameterStream.toList();
    }

    // TODO: Why is this necessary? Why aren't arguments base-symbols by default?
    // TODO Shouldn't DimFunction return dim constant? Shouldn't dim constant be a primitive?
    private Symbol findBaseSymbol(Symbol symbol) {
        if (symbol == null || Objects.equals(symbol.returnType().path(), symbol.path()))
            return symbol;

        return findBaseSymbol(symbol.returnType());
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

    public List<Symbol> getCompatibleSymbols(Symbol symbol) {
        return Collections.unmodifiableList(compatibilityMap.get(symbol.path()));
    }

    public static void main(String[] args) {

        //List<Symbol> symbols = Grammar.grammar().symbols();
        // TODO why is game.functions.dim.DimConstant not in the grammar or the description?
        List<Symbol> symbols = Grammar.grammar().symbols().stream().filter(s -> s.usedInGrammar() || s.usedInDescription() || !s.usedInMetadata()).toList();

        SymbolMapper symbolMapper = new SymbolMapper(symbols);
        System.out.println("Finished mapping symbols. Found " + symbolMapper.parameterMap.values().stream().mapToInt(List::size).sum() + " parameter sets.");


        System.out.println(Grammar.grammar().findSymbolByPath("game.Game").rule().rhs());


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
