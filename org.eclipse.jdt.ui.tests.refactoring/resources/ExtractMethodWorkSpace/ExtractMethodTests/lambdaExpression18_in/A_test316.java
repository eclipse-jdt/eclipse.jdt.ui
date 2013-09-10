package lambdaExpression18_in;

@FunctionalInterface
public interface I1 {
	int foo(int a);
}

interface X {	
	I1 i2= a -> /*[*/ a++; /*]*/
}