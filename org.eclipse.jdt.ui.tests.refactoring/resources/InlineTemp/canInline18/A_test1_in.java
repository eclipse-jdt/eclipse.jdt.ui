package p;

class TestInlineLambda1 {
	
	private Object fun1() {
		final FI fi = x -> x++;
		
		Object fi1 = fi;	// [1]
		Object fi2;
		fi2 = fi;		// [2]	
		
		Object[] a = new Object[] {fi, fi}; // [3]
		Object[][] b = new Object[][] {{fi, fi}, {fi}}; // [4]
		Object[] c = {fi, fi}; // [5]
		Object[][] d = {{fi}, {fi}}; // [6]
	
		int x1 = fun2(fi);	// [7]
		TestInlineLambda1 c1 = new TestInlineLambda1(fi);	// [8]
		F f1 = (fi_p) -> fi;	// [9]
		F f2 = (fi_p) -> {
			return fi;		// [10]
		};
		f1.bar(fi); // [11]
		Object fi4 = true ? fi : fi; // [12]
		return fi;		// [13]
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