package lambdaExpression18_in;

@FunctionalInterface
public interface I1 {
	int foo(int a);
}

class X {	
	void foo() {	
		bar(extracted());
	}

	private I1 extracted() {
		return /*[*/(a) -> a + 10/*]*/;
	}
	
	void bar(I1 i) {
	}
}