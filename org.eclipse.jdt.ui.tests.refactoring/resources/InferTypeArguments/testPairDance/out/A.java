package p;

class PairDance {
    public static void main(String[] args) {
        InvertedPair/*<Integer, Double>*/<Integer, Double> ip= new InvertedPair/*<Integer, Double>*/<Integer, Double>();
        Pair/*<Double, Integer>*/<Double, Integer> p= ip;
        p.setA(Double.valueOf(1.1));
        Double a= ip.getA();
        ip.setB(Integer.valueOf(2));
        System.out.println(ip);
    }
}
