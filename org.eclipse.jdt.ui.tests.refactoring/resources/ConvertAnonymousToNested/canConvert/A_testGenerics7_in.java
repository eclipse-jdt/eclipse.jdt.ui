package p;
//private, static, final
public interface A {}
class Z<E> {
	<T> void m(T t){
		A a = new A(){
			class C<AC> {}
			public <AT> void foo(AT at, T t, E e){}
		};
	}
}
