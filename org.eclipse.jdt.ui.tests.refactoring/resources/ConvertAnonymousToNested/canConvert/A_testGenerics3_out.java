package p;
//public, nonstatic, nonfinal
class A<T>{
	public class Inner extends A<T> {
	}

	void f(){
		new Inner();
	}
}
