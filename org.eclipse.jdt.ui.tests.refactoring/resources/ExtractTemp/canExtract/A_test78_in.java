package p;

class A {
	void f() {
		Object obj= getO() != null
				? getO()
				: new Object();
	}
	static Object getO() {
		return null;
	}
}