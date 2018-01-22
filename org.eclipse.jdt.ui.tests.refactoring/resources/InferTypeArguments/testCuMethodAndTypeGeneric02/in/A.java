package p;

class A {
    void call(Ex ex) {
        ex.method("Eclipse1", Integer.valueOf(1));
        Top top= ex;
        top.method("Eclipse2", Integer.valueOf(2));
    }
}

class Top<TC> {
    <TM> void method(TC cTop, TM mTop) {}
}

class Ex<C extends String> extends Top<C> {
    <M extends Integer> void method(C cEx, M mEx) {}
}
