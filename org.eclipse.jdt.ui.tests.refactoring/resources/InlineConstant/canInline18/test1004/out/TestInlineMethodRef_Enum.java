// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class Test {
	static int m(int x) {
		return x--;
	}		
}

enum E {
	E_C1(Test::m);  // [1] 
	E(FI fi) {}
}

@FunctionalInterface
interface FI {
	int foo(int x);
}