package p;

class A<S> {
	int x;

	protected void m() { 
		this.x++;
	}
}

class B<T> extends A<String> {
}
