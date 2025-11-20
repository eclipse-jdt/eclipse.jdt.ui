package p1;

public class A {
	B<String> b;

	public void method(B<String> k) {
		String j = k.genericField;
	}

}
class B<T> {
	T genericField;
}