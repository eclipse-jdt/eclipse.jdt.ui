package p;

public class A {
	private static String s1 = "1", s2 = "2", s3 = "3";
	
	class B {
		void foo() {
			System.out.println(s3 + s1);
		}
	}
}
