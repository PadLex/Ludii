package approaches.symbolic;

import grammar.Grammar;
import main.StringRoutines;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Symbol;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Maps symbols to the symbols which can be used to initialize them.
 *
 * @author Alexander Padula
 */
public class SymbolMapper {
    public static final MappedSymbol emptySymbol = new EmptySymbol();
    public static final MappedSymbol endOfClauseSymbol = new EndOfClauseSymbol();

    // TODO do I need to add RegionConstant?
    private static final String[] primitives = {
            "java.lang.Integer", "game.functions.ints.IntConstant", "game.functions.dim.DimConstant",
            "java.lang.Float", "game.functions.floats.FloatConstant",
            "java.lang.String",
            "java.lang.Boolean", "game.functions.booleans.BooleanConstant"
    };

    // To obtain every possible set of symbols which can be used to initialize another symbol, you would need replace
    // each base-symbol with it's corresponding source symbols and take their cartesian product. Unfortunately,
    // this is too intensive to pre-compute.
    // Maps symbols to every possible set of base-symbols (aka parameters) that can be used to initialize them.
    // eg game.util.graph.Graph can be initialized using [<Float>, null], [<Float>, <Integer>], [], or [<graph>]
    final Map<String, List<List<MappedSymbol>>> parameterMap = new HashMap<>();
    private final Set<Symbol> symbols = new HashSet<>();
    private final Set<String> paths = new HashSet<>();
    private final Map<String, List<Symbol>> compatibilityMap = new HashMap<>();

    public SymbolMapper() {
        this(Grammar.grammar().symbols().stream().filter(s ->
                (s.usedInGrammar()  // Includes most symbols, including types
                        || !s.usedInMetadata())  // Includes even more types and many constants
                        //&& !s.isAbstract()  // Excludes abstract classes
                        && !s.path().equals("game.rules.start.set.SetStartSitesType.Phase")  // Excluded because its grammar label collides with game.rules.phase.Phase
        ).toList());
    }

    public SymbolMapper(Collection<Symbol> symbols) {
        this.symbols.addAll(symbols);
        this.paths.addAll(symbols.stream().map(Symbol::path).toList());

        buildSymbolMap();
        buildCompatibilityMap();
    }

    /**
     * Primary method of the SymbolMapper.
     *
     * @param parent The symbol to complete.
     * @param partialArguments A list of symbols which have already been selected to complete the parent.
     * @return A list of symbols which could be the next argument for the parent.
     */
    public List<MappedSymbol> nextPossibilities(Symbol parent, List<? extends Symbol> partialArguments) {
        assert !partialArguments.contains(endOfClauseSymbol);
        Stream<List<MappedSymbol>> parameterSets = parameterMap.get(parent.path()).stream();

        parameterSets = parameterSets.filter(completeArguments -> {
            if (partialArguments.size() >= completeArguments.size()) return false;

            for (int i = 0; i < partialArguments.size(); i++) {
                Symbol partialArg = partialArguments.get(i);
                Symbol completeArg = completeArguments.get(i);
                if (!completeArg.compatibleWith(partialArg) || completeArg.nesting() != partialArg.nesting())
                    return false;
            }

            return true;
        });


        Map<String, MappedSymbol> possibilities = new HashMap<>();
        parameterSets.forEach(args -> {
            MappedSymbol argSymbol = args.get(partialArguments.size());
            String argKey = "|" + argSymbol.nesting() + "|" + argSymbol.label;

            if (argSymbol.nesting() > 0) {
                possibilities.put(argSymbol.path() + argKey, argSymbol);
            } else {
                for (Symbol symbol : compatibilityMap.get(argSymbol.path())) {
                    // TODO do I need argSymbol.nesting()
                    possibilities.put(symbol.path() + argKey, new MappedSymbol(symbol, argSymbol.label));
                }
            }
        });

        return possibilities.values().stream().sorted(Comparator.comparing(Symbol::path)).toList();
    }

    public List<Symbol> getCompatibleSymbols(Symbol symbol) {
        return Collections.unmodifiableList(compatibilityMap.get(symbol.path()));
    }

    private void buildCompatibilityMap() {
        for (Symbol symbol : symbols) {
            compatibilityMap.put(symbol.path(), new ArrayList<>());
        }

        for (Symbol symbol : symbols) {
            for (Symbol other : symbols) {
                boolean isCompatible = symbol.cls().isAssignableFrom(other.cls()) || symbol.cls().isAssignableFrom(other.returnType().cls());
                boolean isSubLudeme = other.ludemeType() == Symbol.LudemeType.SubLudeme;
                boolean isInitializable =
                        !Modifier.isAbstract(other.cls().getModifiers())
                                && !Modifier.isInterface(other.cls().getModifiers())
                                && (
                                other.cls().getConstructors().length > 0
                                        || Arrays.stream(other.cls().getMethods()).anyMatch(m -> m.getName().equals("construct"))
                        );
                boolean isEnumValue = other.cls().isEnum() && !other.cls().getTypeName().equals(other.path());
                boolean isPrimitive = Arrays.stream(primitives).anyMatch(p -> p.equals(other.path()));
                boolean inGrammar = other.usedInGrammar();
                boolean hasRule = other.rule() != null;

                if (isCompatible && !isSubLudeme && (isInitializable || isEnumValue) && ((inGrammar && hasRule) || isPrimitive || isEnumValue)) {
                    compatibilityMap.get(symbol.path()).add(other);
                }
            }
        }

        compatibilityMap.put(emptySymbol.path(), List.of(emptySymbol));
        compatibilityMap.put(endOfClauseSymbol.path(), List.of(endOfClauseSymbol));
    }

    private void buildSymbolMap() {
        for (Symbol symbol : symbols) {
            List<List<MappedSymbol>> parameterSets = new ArrayList<>(findParameterSets(symbol));
            parameterSets.sort(Comparator.comparing(List::toString));
            parameterMap.put(symbol.path(), parameterSets);
        }
    }

    private List<List<MappedSymbol>> findParameterSets(Symbol symbol) {
        List<List<MappedSymbol>> constructorSets = new ArrayList<>();

        if (symbol.isTerminal())
            return constructorSets;

        if (symbol.rule() == null)
            return constructorSets;

        for (Clause clause : symbol.rule().rhs()) {
            if (clause.args() == null)
                continue;

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

                // TODO why do some symbols claim not to be part of the grammar despite being part of a clause?
                //if (!arg.symbol().usedInGrammar()) System.out.println("Symbol " + arg.symbol().path() + " is not used in grammar");

                // TODO is if-else correct or should it just be a bunch of ifs?
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

            // Permute optional flags
            // optionalIndexes: [3, 4, 5, 10, 12, 13, 14]
            // possibleSets: [[1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], [1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], ..., [1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1] ]
            List<BitSet> flagPermutations = permuteFlags(optionalFlags, mandatoryFlags);

            // Complete selected andGroups
            completeAndGroups(clause.args(), flagPermutations);

            // Shift selection in or groups
            // MUST BE for i as we are modifying the list
            List<BitSet> shiftedSets = new ArrayList<>();
            for (BitSet flagPermutation : flagPermutations) {
                recursivelyShiftOrGroups(flagPermutation, 1, clause.args(), shiftedSets);
            }

            constructorSets.addAll(shiftedSets.stream().map(set -> {
                List<MappedSymbol> clauseSymbols = new ArrayList<>(clause.args().size() + 1);
                for (int i = 0; i < clause.args().size(); i++) {
                    if (set.get(i)) {
                        ClauseArg arg = clause.args().get(i);
                        String label = arg.label() != null ? StringRoutines.toDromedaryCase(arg.label()) : null;

                        // TODO for some reason nested function clauses also list java primitives as options even when
                        //  they are not compatible. For example, Sites can only be instantiated with an array of
                        //  DimFunction yet one of the possible arguments is an array of java int
                        if (arg.nesting() > 0 && arg.symbol().path().indexOf('.') == -1)
                            continue;

                        clauseSymbols.add(new MappedSymbol(arg.symbol(), arg.nesting(), label)); //TODO: check if label should be lower case
                    } else clauseSymbols.add(emptySymbol);
                }

                clauseSymbols.add(endOfClauseSymbol);
                return clauseSymbols;
            }).toList());
        }

        // filter for out-of-vocabulary symbols and duplicates
        Stream<List<MappedSymbol>> parameterStream = constructorSets.stream().distinct();
        parameterStream = parameterStream.filter(l -> paths.containsAll(l.stream().filter(s -> s != emptySymbol && s != endOfClauseSymbol).map(Symbol::path).toList()));

        return parameterStream.toList();
    }

    private List<BitSet> permuteFlags(BitSet optionalFlags, BitSet mandatoryFlags) {
        List<BitSet> possibleSets = new ArrayList<>();

        int optionalParams = optionalFlags.cardinality();
        int[] optionalIndexes = IntStream.range(0, optionalFlags.length()).filter(optionalFlags::get).toArray();

        int initialPermutations = (int) Math.pow(2, optionalParams);
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

    private void completeAndGroups(List<ClauseArg> clauseArgs, List<BitSet> possibleSets) {
        for (BitSet currentSet : possibleSets) {
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

    private void recursivelyShiftOrGroups(BitSet currentSet, int currentOrGroup, List<ClauseArg> clauseArgs, List<BitSet> possibleSets) {
        int groupIndex = IntStream.range(0, clauseArgs.size()).filter(i -> clauseArgs.get(i).orGroup() == currentOrGroup).findFirst().orElse(-1);

        if (groupIndex == -1) {
            possibleSets.add(currentSet);
            return;
        }

        if (!currentSet.get(groupIndex)) {
            recursivelyShiftOrGroups(currentSet, currentOrGroup + 1, clauseArgs, possibleSets);
            return;
        }

        for (int i = groupIndex; i < clauseArgs.size(); i++) {
            ClauseArg arg = clauseArgs.get(i);
            if (arg.orGroup() != currentOrGroup)
                break;

            BitSet newSet = (BitSet) currentSet.clone();
            newSet.set(groupIndex, false);
            newSet.set(i);
            recursivelyShiftOrGroups(newSet, currentOrGroup + 1, clauseArgs, possibleSets);
        }

    }

    static class EmptySymbol extends MappedSymbol {
        private EmptySymbol() {
            super(null, "mapper.unused", null, SymbolMapper.class, null);
        }

        @Override
        public boolean compatibleWith(final Symbol other) {
            return other instanceof EmptySymbol;
        }

        @Override
        public Symbol returnType() {
            return this;
        }
    }

    static class EndOfClauseSymbol extends MappedSymbol {
        private EndOfClauseSymbol() {
            super(null, "mapper.endOfClause", null, SymbolMapper.class, null);
        }

        @Override
        public boolean compatibleWith(final Symbol other) {
            return other instanceof EndOfClauseSymbol;
        }

        @Override
        public Symbol returnType() {
            return this;
        }
    }

    public static class MappedSymbol extends Symbol {
        public final String label;

        public MappedSymbol(LudemeType type, String path, String alias, Class<?> cls, String label) {
            super(type, path, alias, cls);
            this.label = label;
        }

        public MappedSymbol(MappedSymbol other) {
            super(other);
            this.label = other.label;
        }

        public MappedSymbol(Symbol other, String label) {
            super(other);
            this.label = label;
        }

        public MappedSymbol(Symbol other, int nesting, String label) {
            super(other);
            this.label = label;
            this.setNesting(nesting);
        }

        @Override
        public String toString() {
            return super.path() + "|" + super.nesting();
        }
    }
}
