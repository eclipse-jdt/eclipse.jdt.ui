// 5, 28 -> 5, 33  replaceAll == true, removeDeclaration == false
package p;

class C<T> {
	static final C<String> CONST = new C<>(null);
	
    T field1;
    public C(T param){
        field1 = param;
    }
    public static void main(String[] args) {
        C.testFunction(CONST.getField());
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
