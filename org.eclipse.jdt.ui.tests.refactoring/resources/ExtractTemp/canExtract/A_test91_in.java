package p;

import java.util.ArrayList;

public class A {
	public void foo() {
		ArrayList<? super Integer> nl= new ArrayList<Integer>();
		Object o= nl.get(0);
	}
}
