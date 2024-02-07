package p; //5, 68, 5, 94

public class A {
	int foo(Object obj) {
		return obj == null || ((Integer) obj).intValue() < 0 ? 0 : ((Integer) obj).intValue();
	}
}
