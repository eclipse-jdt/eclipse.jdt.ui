package p;

class A extends ParentClass {
}
class ParentClass {
	int f;
	void method() {
		System.out.println("ParentClass method");
	}
}
class B extends A {
	int f;
	void method() {
		System.out.println("Class B method");
	}
	void m() {
		super.f = 3;
		super.method();
	}
}
