package p;

class A {
    public static final int B= 12;
    public static final int K= 99;

    public static void main(String[] args) {
        int f1= K - 1 - (B - 1);
        int f2= K - (B - 1) - (B - 1) - (B - 1);

        int x1= K + (B - 1);
        int x2= K - (B - 1);
        int x3= K + 1 - (B - 1);
        int x4= K - 1 + (B - 1);
        int x5= K + 1 + (B - 1) - (B - 1) - (B - 1);
    }
}
