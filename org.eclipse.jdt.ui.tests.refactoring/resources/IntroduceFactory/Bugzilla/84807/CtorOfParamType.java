package p;

public class CtorOfParamType<T> {
	public CtorOfParamType(T t) { }
}

class call {
	void foo() {
		CtorOfParamType<String> x= new /*[*/CtorOfParamType/*]*/<String>("");
	}
}