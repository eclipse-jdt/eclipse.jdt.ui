package p;
//public, static, final
class A{
	public final class Inner extends A {
	}

	void f(){
		new Inner();
	}
}