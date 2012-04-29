package p;

public class A extends B{
    B someField;
    static int a;
    String b;
}

class B {
	enum MyEnum {
	    FOO, BAR;

		void someMethod(A a) {
		    switch (this) {
		    case FOO:
		        System.out.println(A.a);
		        break;
		    case BAR:
		        System.out.println(a.b);
		        System.out.println(a.someField);
		    }
		}
	}
}