package p;

class TestInlineMethodReference0 {
	
	private FI fun1() {
		FI fi1 = this::m;	// [1]
		FI fi2;
		fi2 = this::m;		// [2]	
		
		FI[] a = new FI[] {this::m, this::m}; // [3]
		FI[][] b = new FI[][] {{this::m, this::m}, {this::m}}; // [4]
		FI[] c = {this::m, this::m}; // [5]
		FI[][] d = {{this::m}, {this::m}}; // [6]
	
		int x1 = fun2(this::m);	// [7]
		TestInlineMethodReference0 c1 = new TestInlineMethodReference0(this::m);	// [8]
		F f1 = (fi_p) -> this::m;	// [9]
		F f2 = (fi_p) -> {
			return this::m;		// [10]
		};
		f1.bar(this::m); // [11]
		FI fi4 = true ? this::m : this::m; // [12]
		return this::m;		// [13]
	}
	
	private int fun2(FI fi) {return 0;}
	public TestInlineMethodReference0(FI fi) { }
	
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
	FI bar(FI fi);
}