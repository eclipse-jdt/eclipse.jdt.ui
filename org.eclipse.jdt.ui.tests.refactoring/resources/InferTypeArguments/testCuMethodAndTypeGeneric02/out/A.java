package p;

class A {
    void call(Ex<String> ex) {
        ex.method("Eclipse1", new Integer(1));
        Top<String> top= ex;
        top.method("Eclipse2", new Integer(2));
    }
}

class Top<TC> {
    <TM> void method(TC cTop, TM mTop) {}
}

class Ex<C> extends Top<C> {
    <M> void method(C cEx, M mEx) {}
}
