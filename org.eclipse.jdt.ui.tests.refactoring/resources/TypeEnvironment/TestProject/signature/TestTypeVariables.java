package signature;

public class TestTypeVariables<T, E extends String, R extends Object & Comparable<? super R>> {
	void signature() {
		T t= null;
		E e= null;
		R r= null;
	}
}
