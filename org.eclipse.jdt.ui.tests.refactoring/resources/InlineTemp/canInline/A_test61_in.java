package p;

class A {
	int f = 23;
    static class B {
        static int f = 42;
    }
    int foo() {
        int r = B.f;
        A B = this;
        return r;
    }
  }
}