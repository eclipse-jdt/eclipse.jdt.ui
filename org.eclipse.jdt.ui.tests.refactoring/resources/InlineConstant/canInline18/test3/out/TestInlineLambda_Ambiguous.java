// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestClass extends SuperClass {
	{
		bar(0, (FI) x -> x++); // [1]
		super.bar(0, (FI) x -> x++); // [2]
	}
	
	TestClass() {
		this(0, (FI) x -> x++); // [3]
	}
	
	TestClass(int i, FI a) {
		super(i, (FI) x -> x++); // [4]
	}

	TestClass(int i, FX b) { }

	{
		new TestClass(0, (FI) x -> x++); // [5]
	}

	void bar(int x, FX fx) { 
		System.out.println();
	}
}

class SuperClass {
	public SuperClass() { }
	SuperClass(int i, FI fi) { }
	SuperClass(int x, FX fx) { }

	void bar(int i, FI fi) { }
	void bar(int x, FX fx) { }
}

enum E {
	EE(0, (FI) x -> x++); // [6]
	E(int i, FI fi) { }
	E(int s, FX fl) { }
}

@FunctionalInterface
interface FI {
	int foo(int x);
}

@FunctionalInterface
interface FX {
	int foo(String s);
}