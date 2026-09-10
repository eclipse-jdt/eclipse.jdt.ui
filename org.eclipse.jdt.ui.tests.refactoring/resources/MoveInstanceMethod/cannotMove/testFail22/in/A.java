package p1;

class C {
    public void foo() static {
        A source = new A();
        int result = source.m(() -> 7);
        System.out.println(result);
    }
}

class A {
    // Move Instance Method target
    // Move to parameter: target
    int m(B target) {
        return target.value() + 1;
    }
}

@FunctionalInterface
interface B {
    int value();
}
