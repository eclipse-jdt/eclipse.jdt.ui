package p;

class A<S> {
}

class B<T,S> extends A<String> {
	void m(T t) { 
		a();
	}
	private void a(){}
}
