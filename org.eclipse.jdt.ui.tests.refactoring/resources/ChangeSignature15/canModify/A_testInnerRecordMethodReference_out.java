package p;

import java.util.*;

class C {
	public void bar() {
		List<A.B> people = new ArrayList<>();
		Collections.sort(people, Comparator.comparing(A.B::newName)); // Case1 w/o space
		Collections.sort(people, Comparator.comparing(A . B :: newName));// Case2 with space
	}
}

public record A(int a , char c) {
	public record B(int x , String y) {
		String newName() {
			return y;
		}
	}
}