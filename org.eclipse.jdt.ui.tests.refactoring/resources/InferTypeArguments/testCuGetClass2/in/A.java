package p;

class A {
	void m(Object someObject) {
		someObject.getClass();
	}
	void m1(Object someObject) {
		Class c= someObject.getClass();
	}
	void i(Integer someInt) {
		someInt.getClass();
	}
	void i1(Integer someInt) {
		Class c= someInt.getClass();
	}
}
