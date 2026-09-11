package p1;

class C {
    static String trace = "";
    public static void main(String[] args) {
        createSource().m(createTarget()); // Move Instance Method: m()
        System.out.println(trace);
    }
    static A createSource() {
        trace += "S";
        return new Impl();
    }
    static B createTarget() {
        trace += "T";
        return new B();
    }
}
interface A {
    default void m(B b) {
        this.toString();
    }
}
class Impl implements A {
}
class B {
}