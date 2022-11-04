package p; //8, 44, 8, 58

public class A {
	static String str= "str";

	public void foo() {
		if (str == null || A.str.length() > 0) {
			int length= A.str.length();
			System.out.println(str + "," + length);
		}
	}

}
