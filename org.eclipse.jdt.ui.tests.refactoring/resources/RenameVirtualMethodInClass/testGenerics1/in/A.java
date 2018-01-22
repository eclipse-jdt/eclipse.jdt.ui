class Test {
    public static void main(String[] args) {
        new A<Number>().m(Double.valueOf(1));
        new A<Integer>().m(Integer.valueOf(2));

        new Impl().m(Integer.valueOf(3));
        new Impl().m(Float.valueOf(4));
        
        A<Number> a= new Impl();
        a.m(Integer.valueOf(6));
        a.m(Double.valueOf(7));
    }
}


class A<G> {
	void m(G g) { System.out.println("A#m(G): " + g); }
}

class Impl extends A<Number> {
	void m(Integer g) { System.out.println("nonripple Impl#m(Integer): " + g);}
	void m(Number g) { System.out.println("Impl#m(Number): " + g); }
}
