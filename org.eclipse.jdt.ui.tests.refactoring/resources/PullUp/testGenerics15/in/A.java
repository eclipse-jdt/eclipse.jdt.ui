package p;

class A {
    void m() {
        //implementation
    }
}

class C extends A {
}

class B extends C {
	void m() {
		super.m();
		//further implementation
	}
}