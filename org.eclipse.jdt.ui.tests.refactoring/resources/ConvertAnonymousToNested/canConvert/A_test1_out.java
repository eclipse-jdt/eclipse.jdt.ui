package p;
//public, static, final
class A{
	public static final class Inner extends A {
	}
	void f(){
		new Inner();
	}
}