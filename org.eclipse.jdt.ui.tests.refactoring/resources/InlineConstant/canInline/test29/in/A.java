package p;

import java.util.Collections;
import java.util.Map;

class A {
	private static final Map<Float, Float> EMPTY_MAP= Collections.emptyMap();
	
	<T extends Number> A(Map<T, T> map) { }
	
	A() {
		this(EMPTY_MAP);
		Map<Float, Float> emptyMap= EMPTY_MAP;
		Map<?, ?> emptyMap2= EMPTY_MAP;
		new A(EMPTY_MAP);
	}
}
