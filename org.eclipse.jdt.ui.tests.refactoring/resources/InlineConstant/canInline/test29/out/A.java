package p;

import java.util.Collections;
import java.util.Map;

class A {
	<T extends Number> A(Map<T, T> map) { }
	
	A() {
		this(Collections.<Float, Float>emptyMap());
		Map<Float, Float> emptyMap= Collections.emptyMap();
		Map<?, ?> emptyMap2= Collections.emptyMap();
		new A(Collections.<Float, Float>emptyMap());
	}
}
