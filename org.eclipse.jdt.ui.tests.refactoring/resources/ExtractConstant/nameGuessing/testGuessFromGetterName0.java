package p;
class A {
	void foo() {
		String s= getFooBar();//expected FOO_BAR
	}
	static String getFooBar() {
		return null;
	}
}
