package p;

class SubClass extends SuperClass {
	void bar() {
		bar();
		foo();
	}

	private void foo() {
		foo();
		foo();
	}
}
