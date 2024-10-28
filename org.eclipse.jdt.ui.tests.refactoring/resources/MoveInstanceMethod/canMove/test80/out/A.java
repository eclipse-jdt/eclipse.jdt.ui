package p1;

class A {
	B b;
}

class ParentClass {
	void method() {
		System.out.println("ParentClass method");
	}
}

class B extends ParentClass {
	void test() {
		super.method();
	}

	void method() {
		System.out.println("A class method");
	}
}
