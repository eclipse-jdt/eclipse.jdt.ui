// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestClass extends SuperClass {
	private static int m(int x) {
		return x++;
	}
	
	{
		bar(0, (FI) TestClass::m); // [1]
		super.bar(0, (FI) TestClass::m); // [2]
	}
	
	TestClass() {
		this(0, (FI) TestClass::m); // [3]
	}
	
	TestClass(int i, FI a) {
		super(i, (FI) TestClass::m); // [4]
	}

	TestClass(int i, FX b) { }

	{
		new TestClass(0, (FI) TestClass::m); // [5]
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

@FunctionalInterface
interface FI {
int foo(int x);
}

@FunctionalInterface
interface FX {
int foo(String s);
}