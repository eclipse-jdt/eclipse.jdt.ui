package p;

public class A {
    B someField;

    enum MyEnum {
        FOO, BAR
    }

    void someMethod(MyEnum fooBar) {
        switch (fooBar) {
        case FOO:
            System.out.println("foo");
            break;
        case BAR:
            System.out.println("bar");
        }
    }
}

class B {

}