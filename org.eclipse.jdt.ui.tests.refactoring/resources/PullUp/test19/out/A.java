package p;

class A {
    void m() {
        //implementation
    }
}

class C extends A {

	void m() {
		super.m();
		//further implementation
	}
}

class B extends C {
}