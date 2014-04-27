// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestInlineMethodRef {
	private static int m(int x) {
		return x++;
	}

	static { 
		FI a = TestInlineMethodRef::m;	// [1]	
		FI b;
		b = TestInlineMethodRef::m;		// [2]		
	}
	
	private FI fun1() {
		return TestInlineMethodRef::m;	// [3]
	}
	
	FI[] c = new FI[] {TestInlineMethodRef::m, TestInlineMethodRef::m}; // [4]
	FI[][] d = new FI[][] {{TestInlineMethodRef::m, TestInlineMethodRef::m}, {TestInlineMethodRef::m}}; // [5]
	FI[] e = {TestInlineMethodRef::m, TestInlineMethodRef::m}; // [6]
	FI[][] f = {{TestInlineMethodRef::m}, {TestInlineMethodRef::m}}; // [7]
	
	int g = fun2(TestInlineMethodRef::m);	// [8]
	TestInlineMethodRef h = new TestInlineMethodRef(TestInlineMethodRef::m);	// [9]
	private int fun2(FI fi) {return 0;}
	public TestInlineMethodRef(FI fi) {}

	private void fun3() {
		F f1 = (fi_p) -> TestInlineMethodRef::m;	// [10]
		F f2 = (fi_p) -> {
			return TestInlineMethodRef::m;		// [11]
		};
		boolean flag = true;
		FI fi4 = flag ? TestInlineMethodRef::m : TestInlineMethodRef::m; // [12]
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