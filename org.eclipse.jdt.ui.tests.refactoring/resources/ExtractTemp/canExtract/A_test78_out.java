package p;

class A {
	void f() {
		Object o2= getO();
		Object obj= o2 != null
				? o2
				: new Object();
	}
	static Object getO() {
		return null;
	}
}