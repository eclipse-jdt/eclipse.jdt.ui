package p;
//public, nonstatic, final
class A{
	public final class Inner extends A {
	}

	void f(){
		new Inner();
	}
}