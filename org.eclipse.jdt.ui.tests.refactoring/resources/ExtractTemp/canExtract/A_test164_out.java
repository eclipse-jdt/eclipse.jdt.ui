package p;

public class A {	

	public void extractVariableBug() {
		int a = 1;
		Integer b = 2;
		String c = "3";
		String x= b + c;
		System.out.println(a + x);
	}
}