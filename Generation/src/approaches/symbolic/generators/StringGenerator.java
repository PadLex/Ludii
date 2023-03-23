package approaches.symbolic.generators;

import approaches.symbolic.SymbolMapper;
import approaches.symbolic.nodes.EmptyNode;
import approaches.symbolic.nodes.GameNode;
import approaches.symbolic.nodes.GeneratorNode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class StringGenerator {

    GeneratorNode current;

    SymbolMapper symbolMapper;

    public StringGenerator(SymbolMapper symbolMapper) {
        this.symbolMapper = symbolMapper;
    }



    public List<List<GeneratorNode>> nextOptions() {
        if (current == null)
            return List.of(List.of(new GameNode()));

        List<List<GeneratorNode>> options = new ArrayList<>();
        options.add(current.nextPossibleParameters(symbolMapper));
        while (options.get(options.size()-1).remove(EmptyNode.instance)) {
            current.addParameter(EmptyNode.instance);
            options.add(current.nextPossibleParameters(symbolMapper));
        }

        return options;
    }

}
