package p;
class A<T> {
    T field1;
    public A(T param){
        field1 = param;
    }
    public static void main(String[] args) {
        A.testFunction(new A<>(null).getField());
    }
    public static void testFunction(String param){
        System.out.println("S " + param);
    }
    public static void testFunction(Object param){
        System.out.println("O " + param);
    }
    public T getField(){
        return field1;
    }
}
