package p;
class A<T>{
	private T f;
	class B<T>{
		static <S> S f(S t) {
			T s=t;
			return null;
		}
	}
}