package p;
// Class A
public record A(int a, String b) {
	public A(String b) {
		this(3, b);
	}
	public A() {
		this(3, "abc");
	}
}