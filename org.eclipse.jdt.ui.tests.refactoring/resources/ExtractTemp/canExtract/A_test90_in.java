package p;

import java.util.ArrayList;

public class A {
	public void foo() {
		ArrayList<? extends Number> nl= new ArrayList<Integer>();
		Number n= nl.get(0);
	}
}
