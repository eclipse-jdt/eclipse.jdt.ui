package p;

class A {
	void foo() {
		final int value= 42;//temp comment
		// some valuable important comment which will be erased
		System.out.println(value);
	}
}