package p;

class A<T> {
	protected int j;
	void a(T t){}
	protected void m() { 
		this.j++;
		this.j= 0;
	}
}

class B extends A<Object> {
}