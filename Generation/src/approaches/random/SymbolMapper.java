package approaches.random;

import grammar.Grammar;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Symbol;

import java.util.*;
import java.util.stream.IntStream;

public class SymbolMapper {

    private static HashMap<Symbol, List<List<Symbol>>> symbolsMap = new HashMap<>();

    private static List<List<Symbol>> findParameterSets(Symbol symbol) {
        List<List<Symbol>> parameterSets = new ArrayList<>();

        for (Clause clause: symbol.rule().rhs()) {
            if (clause.args() == null) {
                continue;
            }
            System.out.println("\n");

            System.out.println(clause);
            System.out.println("args:    " + clause.args());
            System.out.println("or:      " + clause.args().stream().map(ClauseArg::orGroup).toList());
            System.out.println("and:     " + clause.args().stream().map(ClauseArg::andGroup).toList());
            System.out.println("not opt: " + clause.args().stream().map(arg -> arg.optional()? 0:1).toList());

            System.out.println("man:     " + IntStream.range(0, clause.mandatory().length())
                    .mapToObj(b -> String.valueOf(clause.mandatory().get(b) ? 1 : 0)).toList());

            // Find optional flags
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

            System.out.println("opt flag: " + IntStream.range(0, optionalFlags.length())
                    .mapToObj(b -> String.valueOf(optionalFlags.get(b) ? 1 : 0)).toList());

            System.out.println("man flag: " + IntStream.range(0, mandatoryFlags.length())
                    .mapToObj(b -> String.valueOf(mandatoryFlags.get(b) ? 1 : 0)).toList());

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

            System.out.println("test:    " + IntStream.range(0, optionalFlags.length())
                    .mapToObj(b -> String.valueOf(possibleSets.get(possibleSets.size()-1).get(b) ? 1 : 0)).toList());
            List<BitSet> test = new ArrayList<>();
            recursivelyShiftOrGroups(possibleSets.get(possibleSets.size()-1), 1, clause.args(), test);
            System.out.println("result: " + test.stream().map(set -> IntStream.range(0, clause.args().size())
                    .mapToObj(b -> String.valueOf(set.get(b) ? 1 : 0)).toList()).toList());

            // check for duplicate sets
            System.out.println("uniqueness check: " + (possibleSets.size() == possibleSets.stream().distinct().count()));

            System.out.println(possibleSets.size());

            System.out.println("possibleSets: " + possibleSets.stream().map(set -> IntStream.range(0, clause.args().size())
                    .mapToObj(b -> String.valueOf(set.get(b) ? 1 : 0)).toList()).toList());
        }

        return parameterSets;
    }

    private static List<BitSet> permuteFlags(BitSet optionalFlags, BitSet mandatoryFlags) {
        List<BitSet> possibleSets = new ArrayList<>();

        int optionalParams = optionalFlags.cardinality();
        int[] optionalIndexes = IntStream.range(0, optionalFlags.length()).filter(optionalFlags::get).toArray();
        System.out.println("optionalIndexes: " + Arrays.toString(optionalIndexes));

        int initialPermutations = (int) Math.pow(2, optionalParams);
        System.out.println("initialPermutations: " + initialPermutations);
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

        Symbol symbol = Grammar.grammar().symbolsByName("Place").get(0);
        System.out.println(findParameterSets(symbol));

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
