package p;

public class CtorOfParamType<T> {
	public static <T> CtorOfParamType<T> createCtorOfParamType(T t) {
		return new CtorOfParamType<T>(t);
	}

	private CtorOfParamType(T t) { }
}

class call {
	void foo() {
		CtorOfParamType<String> x= CtorOfParamType.createCtorOfParamType("");
	}
}