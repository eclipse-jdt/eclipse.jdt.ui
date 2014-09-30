package p; //6, 51, 6, 57

import java.util.function.Consumer;

public class A {
	Consumer<Integer> a1= x -> {
		int x2= x + 10;
		System.out.println(x2);
	};
}
