package p;

class A {
	int f = 23;
    static class B {
        static int f = 42;
    }
    int foo() {
        A B = this;
        return A.B.f;
    }
  }
}