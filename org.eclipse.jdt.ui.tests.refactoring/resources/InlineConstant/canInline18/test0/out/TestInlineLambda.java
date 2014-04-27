// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestInlineLambda {
	static { 
		FI a = x -> x++;	// [1]	
		FI b;
		b = x -> x++;		// [2]		
	}
	
	private FI fun1() {
		return x -> x++;	// [3]
	}
	
	FI[] c = new FI[] {x -> x++, x -> x++}; // [4]
	FI[][] d = new FI[][] {{x -> x++, x -> x++}, {x -> x++}}; // [5]
	FI[] e = {x -> x++, x -> x++}; // [6]
	FI[][] f = {{x -> x++}, {x -> x++}}; // [7]
	
	int g = fun2(x -> x++);	// [8]
	TestInlineLambda h = new TestInlineLambda(x -> x++);	// [9]
	private int fun2(FI fi) {return 0;}
	public TestInlineLambda(FI fi) {}

	private void fun3() {
		F f1 = (fi_p) -> x -> x++;	// [10]
		F f2 = (fi_p) -> {
			return x -> x++;		// [11]
		};
		boolean flag = true;
		FI fi4 = flag ? x -> x++ : x -> x++; // [12]
	}

	enum E {
		E_C1(x -> x++);  // [13]
		E(FI fi) {
		}
	}

}

enum E1 {
	E_C1(x -> x++); // [14]
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