package p;

class A<S> {
	int x;
}

class B<T> extends A<String> {
	protected void m() { 
		super.x++;
	}
}
