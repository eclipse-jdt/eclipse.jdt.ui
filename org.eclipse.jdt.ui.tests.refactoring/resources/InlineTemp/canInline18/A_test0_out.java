package p;

class TestInlineLambda0 {
	
	private FI fun1() {
		FI fi1 = x -> x++;	// [1]
		FI fi2;
		fi2 = x -> x++;		// [2]	
		
		FI[] a = new FI[] {x -> x++, x -> x++}; // [3]
		FI[][] b = new FI[][] {{x -> x++, x -> x++}, {x -> x++}}; // [4]
		FI[] c = {x -> x++, x -> x++}; // [5]
		FI[][] d = {{x -> x++}, {x -> x++}}; // [6]
	
		int x1 = fun2(x -> x++);	// [7]
		TestInlineLambda0 c1 = new TestInlineLambda0(x -> x++);	// [8]
		F f1 = (fi_p) -> x -> x++;	// [9]
		F f2 = (fi_p) -> {
			return x -> x++;		// [10]
		};
		f1.bar(x -> x++); // [11]
		FI fi4 = true ? x -> x++ : x -> x++; // [12]
		return x -> x++;		// [13]
	}
	
	private int fun2(FI fi) {return 0;}
	public TestInlineLambda0(FI fi) { }
}

@FunctionalInterface
interface FI {
	int foo(int x);
}

@FunctionalInterface
interface F {
	FI bar(FI fi);
}