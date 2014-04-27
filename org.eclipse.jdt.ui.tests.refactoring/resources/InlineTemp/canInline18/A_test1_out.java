package p;

class TestInlineLambda1 {
	
	private Object fun1() {
		Object fi1 = (FI) x -> x++;	// [1]
		Object fi2;
		fi2 = (FI) x -> x++;		// [2]	
		
		Object[] a = new Object[] {(FI) x -> x++, (FI) x -> x++}; // [3]
		Object[][] b = new Object[][] {{(FI) x -> x++, (FI) x -> x++}, {(FI) x -> x++}}; // [4]
		Object[] c = {(FI) x -> x++, (FI) x -> x++}; // [5]
		Object[][] d = {{(FI) x -> x++}, {(FI) x -> x++}}; // [6]
	
		int x1 = fun2((FI) x -> x++);	// [7]
		TestInlineLambda1 c1 = new TestInlineLambda1((FI) x -> x++);	// [8]
		F f1 = (fi_p) -> ((FI) x -> x++);	// [9]
		F f2 = (fi_p) -> {
			return (FI) x -> x++;		// [10]
		};
		f1.bar((FI) x -> x++); // [11]
		Object fi4 = true ? (FI) x -> x++ : (FI) x -> x++; // [12]
		return (FI) x -> x++;		// [13]
	}
	
	private int fun2(Object fi) {return 0;}
	public TestInlineLambda1(Object fi) { }
}

@FunctionalInterface
interface FI {
	int foo(int x);
}

@FunctionalInterface
interface F {
	Object bar(Object fi);
}