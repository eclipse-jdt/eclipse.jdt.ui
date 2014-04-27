// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestInlineMethodRef {
	public static final FI fi = TestInlineMethodRef::m;
	
	private static int m(int x) {
		return x++;
	}

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
	TestInlineMethodRef h = new TestInlineMethodRef(fi);	// [9]
	private int fun2(FI fi) {return 0;}
	public TestInlineMethodRef(FI fi) {}

	private void fun3() {
		F f1 = (fi_p) -> fi;	// [10]
		F f2 = (fi_p) -> {
			return fi;		// [11]
		};
		boolean flag = true;
		FI fi4 = flag ? fi : fi; // [12]
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