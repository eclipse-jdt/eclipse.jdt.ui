package p;

class A {
	int f = 23;
    static class B {
    	static class C {
    		static int f = 92;
    	}
    }

	A C;

    int foo() {
		int r = B.C.f;
        A B = this;
		return r;
    }
}