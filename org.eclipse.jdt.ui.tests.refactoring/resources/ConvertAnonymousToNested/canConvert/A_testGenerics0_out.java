package p;
//private, static, final
class A<T>{
	private final class Inner extends A<T> {
	}

	void f(){
		new Inner();
	}
}
