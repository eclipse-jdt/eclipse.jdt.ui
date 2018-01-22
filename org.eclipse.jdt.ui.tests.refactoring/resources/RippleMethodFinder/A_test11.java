package p;

class A {
    public static void main(String[] args) {
        new Generic<Number>().take(Double.valueOf(1));
        new Generic<Integer>().take(Integer.valueOf(2));

        new Impl().take(Integer.valueOf(3));
        new Impl().take(Float.valueOf(4));
        
        Generic<Number> gn= new Impl();
        gn.take(Integer.valueOf(6));
        gn.take(Double.valueOf(7));
    }
}


class Generic<G> {
	void /*target*/take(G g) { System.out.println("Generic#take(G): " + g); }
}

class Impl extends Generic<Number> {
	void take(Integer g) { System.out.println("nonripple Impl#take(Integer): " + g);}
	void /*ripple*/take(Number g) { System.out.println("Impl#take(Number): " + g); }
}
