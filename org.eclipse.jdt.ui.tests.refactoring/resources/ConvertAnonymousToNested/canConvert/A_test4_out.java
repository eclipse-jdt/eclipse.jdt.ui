package p;
//private, static, final
class A{
	private final class Inner extends A {
	}
	A(){
	}
	void f(){
		new Inner();
	}
}