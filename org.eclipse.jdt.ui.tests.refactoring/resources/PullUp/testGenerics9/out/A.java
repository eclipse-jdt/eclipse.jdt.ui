package p;

class A<T> {
	void a(T t){}

	public void m() { 
		this.a(null);
		this.a(new String());
	}
}

class B extends A<String> {
}