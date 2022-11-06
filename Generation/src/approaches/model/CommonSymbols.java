package approaches.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import graphics.svg.SVGLoader;
import main.grammar.Symbol;

public class CommonSymbols {
	


	public static void main(String[] args) {
		System.out.println("HI");
		System.out.println(Arrays.asList(SVGLoader.listSVGs()));

		System.out.println(Arrays.asList(SVGLoader.listSVGs()).stream().map(x -> x.replaceAll("/.*/", "").replaceAll(".svg", "")).collect(HashSet::new, HashSet::add, HashSet::addAll));

	}

}
