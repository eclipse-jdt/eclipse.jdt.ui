class B {
    public void f() {

    }

	public void m(A a) {
	    a.n(a.c);
	}
}

class A {
    B b;
    int c;
    
    void n(int x) {
        
    }
}
