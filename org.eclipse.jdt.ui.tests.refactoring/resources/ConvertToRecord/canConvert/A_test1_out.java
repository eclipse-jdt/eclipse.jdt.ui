package p;
// Class A
public class A {

	/** 
	 * Inner
	 */
	static record Inner (int a, String b) {}
	
	public void foo() {
		Inner x= new Inner(3, "abc");
		System.out.println(x.a());
		System.out.println(x.b());
	}
}