package p;

class A {
	void m(String y){
        m(y);
    }
}

class B {
	public int m(String q) {
		new A().m("x");
		return m("k");
	}
}