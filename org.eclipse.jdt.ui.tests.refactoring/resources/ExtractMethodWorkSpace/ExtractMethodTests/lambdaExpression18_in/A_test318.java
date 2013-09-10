package lambdaExpression18_in;

@FunctionalInterface
public interface I {
	int foo(int a);
}

interface I_Test {
	I i = a -> /*]*/{
		return Integer.valueOf(a);
	};/*[*/
}