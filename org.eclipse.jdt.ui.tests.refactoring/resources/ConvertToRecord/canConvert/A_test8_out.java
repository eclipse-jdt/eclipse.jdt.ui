package p;
// Class A
public record A(int a, String b) {
	public A(int a) {
		this(a, "abc");
	}
}