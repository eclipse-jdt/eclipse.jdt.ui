package p;

class A {
    protected void m() {
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