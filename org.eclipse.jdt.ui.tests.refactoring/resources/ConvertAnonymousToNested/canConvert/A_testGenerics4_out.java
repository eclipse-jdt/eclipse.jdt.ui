package p;
//private, static, final
class A<T>{
	private final class Inner extends A<T> {
	}
	A(){
	}
	void f(){
		new Inner();
	}
}
