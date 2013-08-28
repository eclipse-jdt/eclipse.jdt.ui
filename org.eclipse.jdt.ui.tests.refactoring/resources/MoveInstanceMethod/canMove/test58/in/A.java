package p;

import static java.util.Arrays.asList;

import java.util.List;

public class A {
	public List<String> abc(B b) {
		return asList("a", b.toString());
	}
}