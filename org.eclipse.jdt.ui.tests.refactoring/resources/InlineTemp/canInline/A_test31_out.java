package p;

import java.util.Collections;
import java.util.Map;

class A extends Sup<String> {
	public A(Map<String, Integer> map) {
		method(Collections.emptyMap(/*nada*/));
		method2(Collections.emptyMap(/*nada*/));
		method3(Collections.emptyMap(/*nada*/));
		new A(Collections.emptyMap(/*nada*/));
		super.sup(Collections.emptyMap(/*nada*/));
		
		Map<String, Integer> emptyMap2= Collections.emptyMap(/*nada*/);
		emptyMap2= Collections.emptyMap(/*nada*/);
		Object o= Collections.emptyMap(/*nada*/);
		Map<? extends String, ?> emptyMap3= Collections.emptyMap(/*nada*/);
		
		Integer first= Collections.<String, Integer>emptyMap(/*nada*/).values().iterator().next();
	}

	void method(Map<String, Integer> map) { }
	void method2(Map<? extends String, ?> map) {	}
	void method3(Object map) {	}
}

class Sup<S> {
	<A extends S, B> void sup(Map<A, B> map) {}
}
