package p;

class A<T> {
	void a(A<T> a){}
}

class B<T> extends A<T> {
	protected void m() { 
		a(this);
	}
}