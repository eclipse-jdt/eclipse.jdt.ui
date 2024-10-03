package p;

class A extends ParentClass {
	void m() {
		f = 3;
		method();
	}
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
}
