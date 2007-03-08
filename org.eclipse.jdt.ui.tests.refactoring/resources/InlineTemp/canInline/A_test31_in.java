package p;

import java.util.Collections;
import java.util.Map;

class A extends Sup<String> {
	public A(Map<String, Integer> map) {
		Map<String, Integer> emptyMap= Collections.emptyMap(/*nada*/);
		method(emptyMap);
		method2(emptyMap);
		method3(emptyMap);
		new A(emptyMap);
		super.sup(emptyMap);
		
		Map<String, Integer> emptyMap2= emptyMap;
		emptyMap2= emptyMap;
		Object o= emptyMap;
		Map<? extends String, ?> emptyMap3= emptyMap;
		
		Integer first= emptyMap.values().iterator().next();
	}

	void method(Map<String, Integer> map) { }
	void method2(Map<? extends String, ?> map) {	}
	void method3(Object map) {	}
}

class Sup<S> {
	<A extends S, B> void sup(Map<A, B> map) {}
}
