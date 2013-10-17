package p;

public class Test<T extends Test<T>> {
    void foo(T t) {
    }

    void f(T t) {
        t.foo(null);
    }
}