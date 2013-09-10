package lambdaExpression18_in;

@FunctionalInterface
public interface I {
	int foo(int a);
}

class C_Test {
	I i= a -> {
		/*[*/ return a++; /*]*/ 
	};
}