package p;
//private, static, final
class A{
	private final class Inner extends A {
	}

	void f(){
		new Inner();
	}
}