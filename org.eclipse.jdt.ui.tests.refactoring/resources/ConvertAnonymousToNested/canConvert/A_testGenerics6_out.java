package p;
//private, static, final
class A<T>{
	private static final class Inner<S, T> extends A<S> {
		T t;
	}
	A(){
	}
	<S> void f(){
		new Inner<S, T>();
	}
}
