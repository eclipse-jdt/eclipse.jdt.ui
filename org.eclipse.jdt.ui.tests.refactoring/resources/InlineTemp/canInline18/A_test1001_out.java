package p;

class TestInlineMethodReference1 {
	
	private Object fun1() {
		Object fi1 = (FI) this::m;	// [1]
		Object fi2;
		fi2 = (FI) this::m;		// [2]	
		
		Object[] a = new Object[] {(FI) this::m, (FI) this::m}; // [3]
		Object[][] b = new Object[][] {{(FI) this::m, (FI) this::m}, {(FI) this::m}}; // [4]
		Object[] c = {(FI) this::m, (FI) this::m}; // [5]
		Object[][] d = {{(FI) this::m}, {(FI) this::m}}; // [6]
	
		int x1 = fun2((FI) this::m);	// [7]
		TestInlineMethodReference1 c1 = new TestInlineMethodReference1((FI) this::m);	// [8]
		F f1 = (fi_p) -> ((FI) this::m);	// [9]
		F f2 = (fi_p) -> {
			return (FI) this::m;		// [10]
		};
		f1.bar((FI) this::m); // [11]
		Object fi4 = true ? (FI) this::m : (FI) this::m; // [12]
		return (FI) this::m;		// [13]
	}
	
	private int fun2(Object fi) {return 0;}
	public TestInlineMethodReference1(Object fi) { }
	
	int m(int x) {
		return x++;
	}
}

@FunctionalInterface
interface FI {
	int foo(int x);
}

@FunctionalInterface
interface F {
	Object bar(Object fi);
}