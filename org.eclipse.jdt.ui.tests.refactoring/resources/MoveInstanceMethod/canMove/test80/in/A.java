package p1;

class A {
	B b;

	void method() {
		System.out.println("A class method");
	}
}

class ParentClass {
	void method() {
		System.out.println("ParentClass method");
	}
}

class B extends ParentClass {
	void test() {
		method();
	}
}
