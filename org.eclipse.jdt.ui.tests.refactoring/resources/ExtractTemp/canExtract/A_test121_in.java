package p; //8, 44, 8, 58

public class A {
	static String str= "str";

	public void foo() {
		if (str == null || A.str.length() > 0)
			System.out.println(str + "," + A.str.length());
	}

}
