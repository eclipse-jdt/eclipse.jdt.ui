package p; //8, 32, 8, 44

import java.util.Collection;

class A {
	private void test(Collection c) {
		for (Object o : c) {
			final String temp= o.toString();
			System.out.println(temp);
		}
	}
}
