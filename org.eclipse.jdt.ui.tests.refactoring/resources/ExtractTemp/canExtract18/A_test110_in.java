package p; //6, 77, 6, 82

import java.util.function.BiFunction;

public class A {
	BiFunction<Integer, Integer, Integer> a1= (Integer i, Integer j) -> foo(i + j);

	private int foo(int x) {
		return x;
	}
}
