package p;
class A<X>{	
	void x(){}

	protected void mmm(X t) {}

	protected void n() {}
}
class B<T> extends A<T>{
}
class C<S> extends A<S>{
	protected void mmm(S s){}
}