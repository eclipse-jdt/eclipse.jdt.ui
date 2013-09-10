package lambdaExpression18_in;

@FunctionalInterface
public interface I {
	int foo(int a);
}

interface C_Test {
	default String foo() {
		I i= a -> /*[*/ a++; /*]*/	
		return "";
	}	
}