package p;

class A<S> {
}

class B<T,S> extends A<T> {
	void m(S s) { 
		a();
	}
	private void a(){}
}
