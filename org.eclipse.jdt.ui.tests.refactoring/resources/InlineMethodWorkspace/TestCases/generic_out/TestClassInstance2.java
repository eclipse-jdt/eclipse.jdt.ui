package generic_out;

public class TestClassInstance2<T, E> {
}

class TestClassInstance2References {
	void bar() {
		TestClassInstance2<String, Number> x= new TestClassInstance2<String, Number>();
		String t= null;
		Number e= null;
	}
	void baz() {
		TestClassInstance2<Integer, Boolean> x= new TestClassInstance2<Integer, Boolean>();
		Integer t= null;
		Boolean e= null;
	}
}
