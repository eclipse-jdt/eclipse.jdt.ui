package p;

class A {
	void m(String y){
        m(y);
    }
}

class B extends A {
	public void m(String q) {
		new A().m("x");
		return m("k");
	}
}