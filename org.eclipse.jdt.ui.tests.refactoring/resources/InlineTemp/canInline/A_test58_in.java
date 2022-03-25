package p;
public class A {
        static class B {
                static int f = 22;
                static A B = new A();
        }
        static int f = 42;
        static Runnable foo() {
                Runnable r = new Runnable() {
					@Override
					public void run() {
						int r = B.f;
						
					}
				};
                A B = new A();
                return r;
        }
}