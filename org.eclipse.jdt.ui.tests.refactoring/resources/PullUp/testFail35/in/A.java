package p;

class A {
	void method() {
		System.out.println("ParentClass method");
	}
}

class B extends SubClass {
	void method() {
		System.out.println("Class B method");
	}
}

class SubClass extends A {
	void test() {
		method();
	}
}
