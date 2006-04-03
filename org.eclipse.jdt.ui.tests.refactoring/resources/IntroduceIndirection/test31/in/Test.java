package p;

public class Test {
    protected int foo() {
            return 1;
    }
    int useFoo() {
            return foo();
    }
}
class TestO extends Test {
    int useFoo() {
            return foo();
    }
}