package p;

class A<T> {
	void a(T t){}
}

class B extends A<String> {
	public void m() { 
		super.a(null);
		super.a(new String());
	}
}