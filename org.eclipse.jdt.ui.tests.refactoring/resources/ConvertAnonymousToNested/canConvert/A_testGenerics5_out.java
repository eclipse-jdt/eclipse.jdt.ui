package p;
//private, static, final
class A<T>{
	private static final class Inner<S> extends A<S> {
	}
	A(){
	}
	static <S> void f(){
		new Inner<S>();
	}
}
