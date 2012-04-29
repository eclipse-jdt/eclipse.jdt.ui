package p;

public class A extends B{
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
            System.out.println(someField);
        }
    }
}

class B {
	enum MyEnum {
	    FOO, BAR
	}
}