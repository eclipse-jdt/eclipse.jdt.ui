package p;
class A {
	void foo() {
		boolean isfb= isFooBar();//expected FOO_BAR
	}
	static boolean isFooBar() {
		return false;
	}
}
