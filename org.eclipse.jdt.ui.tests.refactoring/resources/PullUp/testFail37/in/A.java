package p;

public class A {
    public class BaseInner {
        void innerMethodLambda(Outer outer) {
            Runnable r = () -> {
                System.out.println(outer.x);
                outer.foo();
            };
            r.run();
        }
    }

    public class Outer {
        public int x = 0;
        public void foo(){};

        public class Inner extends BaseInner {
            void innerMethod() { // Pull this method up to class BaseInner
				innerMethodLambda(Outer.this);
            }
        }
    }
}