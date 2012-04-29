package p;

public class A {
    B someField;
    static int a;
    String b;

    void someMethod(MyEnum fooBar) {
        switch (fooBar) {
        case FOO:
            System.out.println(a);
            break;
        case BAR:
            System.out.println(b);
        }
    }
}

class B {

}