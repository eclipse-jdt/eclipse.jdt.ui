package lambdaExpression18_in;

@FunctionalInterface
public interface I1 {
	int foo(int a);
}

class X {
	void foo() {	
		I1 i1= /*]*/a -> {
			return a + 10;
			}/*[*/ ;
	}
}