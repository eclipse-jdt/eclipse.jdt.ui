package p;
//public, nonstatic, nonfinal
class A{
	public class Inner extends A {
	}

	void f(){
		new Inner();
	}
}