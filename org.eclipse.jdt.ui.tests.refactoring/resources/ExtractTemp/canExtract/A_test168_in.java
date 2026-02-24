package p;

public class A {	

	public void extractVariableBug() {
		int a = 1;
		Integer b = 2;
		Integer c = 3;
		String d = "3";
		System.out.println(a + d + a + c);
	}
}