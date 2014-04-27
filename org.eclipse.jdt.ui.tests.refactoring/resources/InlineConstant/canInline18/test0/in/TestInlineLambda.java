// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestInlineLambda {
	public static final FI fi = x -> x++;

	static { 
		FI a = fi;	// [1]	
		FI b;
		b = fi;		// [2]		
	}
	
	private FI fun1() {
		return fi;	// [3]
	}
	
	FI[] c = new FI[] {fi, fi}; // [4]
	FI[][] d = new FI[][] {{fi, fi}, {fi}}; // [5]
	FI[] e = {fi, fi}; // [6]
	FI[][] f = {{fi}, {fi}}; // [7]
	
	int g = fun2(fi);	// [8]
	TestInlineLambda h = new TestInlineLambda(fi);	// [9]
	private int fun2(FI fi) {return 0;}
	public TestInlineLambda(FI fi) {}

	private void fun3() {
		F f1 = (fi_p) -> fi;	// [10]
		F f2 = (fi_p) -> {
			return fi;		// [11]
		};
		boolean flag = true;
		FI fi4 = flag ? fi : fi; // [12]
	}

	enum E {
		E_C1(fi);  // [13]
		E(FI fi) {
		}
	}

}

enum E1 {
	E_C1(TestInlineLambda.fi); // [14]
	E1(FI fi) {
	}
}

@FunctionalInterface
interface FI {
	int foo(int x);
}

@FunctionalInterface
interface F {
	FI bar(Object o);
}