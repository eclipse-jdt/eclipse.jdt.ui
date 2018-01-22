class Test {
    public static void main(String[] args) {
        new A<Number>().k(Double.valueOf(1));
        new A<Integer>().k(Integer.valueOf(2));

        new Impl().m(Integer.valueOf(3));
        new Impl().k(Float.valueOf(4));
        
        A<Number> a= new Impl();
        a.k(Integer.valueOf(6));
        a.k(Double.valueOf(7));
    }
}


class A<G> {
	void k(G g) { System.out.println("A#m(G): " + g); }
}

class Impl extends A<Number> {
	void m(Integer g) { System.out.println("nonripple Impl#m(Integer): " + g);}
	void k(Number g) { System.out.println("Impl#m(Number): " + g); }
}
