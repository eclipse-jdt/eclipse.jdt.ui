package p;

class A<T> {
	void a(A<T> a){}

	protected void m() { 
		a(this);
	}
}

class B<T> extends A<T> {
}