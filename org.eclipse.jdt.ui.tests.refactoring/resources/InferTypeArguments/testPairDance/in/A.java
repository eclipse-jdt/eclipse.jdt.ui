package p;

class PairDance {
    public static void main(String[] args) {
        InvertedPair/*<Integer, Double>*/ ip= new InvertedPair/*<Integer, Double>*/();
        Pair/*<Double, Integer>*/ p= ip;
        p.setA(Double.valueOf(1.1));
        Double a= (Double) ip.getA();
        ip.setB(Integer.valueOf(2));
        System.out.println(ip);
    }
}
