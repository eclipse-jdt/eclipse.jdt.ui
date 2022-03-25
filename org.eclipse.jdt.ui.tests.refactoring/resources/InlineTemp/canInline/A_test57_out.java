package p;
public class A {
        static class B {
                static int f = 22;
                static A B = new A();
        }
        static int f = 42;
        static Runnable foo() {
                A B = new A();
                return new Runnable() {
					A B = new A();
					@Override
					public void run() {
						int r = A.f;
					}
				};
        }
}