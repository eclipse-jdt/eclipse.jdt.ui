package p; //6, 73, 6, 83

import java.util.function.BiFunction;

public class A {
	BiFunction<Integer, Integer, Integer> a1= (Integer i, Integer j) -> foo(i + j);

	private int foo(int x) {
		return x;
	}
}
