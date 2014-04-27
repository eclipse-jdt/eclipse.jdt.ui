// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestClass extends SuperClass {
	public static final FI fi = TestClass::m;
	private static int m(int x) {
		return x++;
	}
	
	{
		bar(0, fi); // [1]
		super.bar(0, fi); // [2]
	}
	
	TestClass() {
		this(0, fi); // [3]
	}
	
	TestClass(int i, FI a) {
		super(i, fi); // [4]
	}

	TestClass(int i, FX b) { }

	{
		new TestClass(0, fi); // [5]
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