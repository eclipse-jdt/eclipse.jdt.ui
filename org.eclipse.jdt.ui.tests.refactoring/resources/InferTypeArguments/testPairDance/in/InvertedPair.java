package p;

class InvertedPair<A, B> extends Pair<B, A> {
    public B getA() {
        return super.getA();
    }
    public void setB(A bee) {
        super.setB(bee);
    }
}
