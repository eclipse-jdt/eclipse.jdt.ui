package lambdaExpression18_in;

@FunctionalInterface
public interface I {
	int foo(int a);
}

class C_Test {
	String foo() {
		I i= a -> {
			/*[*/ return a++; /*]*/ 	
		};	
		return "";
	}	
}