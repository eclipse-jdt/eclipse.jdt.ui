package p;
class A<X>{	
	void x(){}

	protected void n() {}

	protected void mmm(X t) {}
}
class B<T> extends A<T>{
}
class C<S> extends A<S>{
	protected void mmm(S s){}
}