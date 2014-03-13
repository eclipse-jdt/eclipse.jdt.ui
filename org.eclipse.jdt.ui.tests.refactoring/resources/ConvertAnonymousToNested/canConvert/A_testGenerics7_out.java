package p;
//private, static, final
public interface A {}
class Z<E> {
	private static final class Inner<T, E> implements A {
		class C<AC> {}
		public <AT> void foo(AT at, T t, E e){}
	}

	<T> void m(T t){
		A a = new Inner<T, E>();
	}
}
