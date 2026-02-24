package p;

public class A {	

	public void extractVariableBug() {
		int a = 1;
		Integer b = 2;
		Integer c = 3;
		System.out.println(a + b.intValue() + c.toString());
	}
}