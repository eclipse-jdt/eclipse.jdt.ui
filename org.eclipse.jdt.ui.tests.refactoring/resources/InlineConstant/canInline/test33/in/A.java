package p;

class A {
    public static final int B= 12;
    public static final int C= B - 1; //inline C
    public static final int K= 99;

    public static void main(String[] args) {
        int f1= K - 1 - C;
        int f2= K - C - C - C;

        int x1= K + C;
        int x2= K - C;
        int x3= K + 1 - C;
        int x4= K - 1 + C;
        int x5= K + 1 + C - C - C;
    }
}
