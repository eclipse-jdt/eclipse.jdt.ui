package p;

class A<T> {
	protected int j;
	void a(T t){}
}

class B extends A<Object> {
	protected void m() { 
		super.j++;
		super.j= 0;
	}
}