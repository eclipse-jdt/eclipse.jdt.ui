package p;

public class Pair<A, B> {
    private A a;
    private B b;
    
    public A getA() {
        return a;
    }
    public void setA(A a) {
        this.a= a;
    }
    public B getB() {
        return b;
    }
    public void setB(B bee) {
        b= bee;
    }
    public String toString() {
        return super.toString() + ", a=" + a + ", b=" + b;
    }
}
