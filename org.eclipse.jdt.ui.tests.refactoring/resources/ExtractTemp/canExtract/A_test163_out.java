package p;

public class A {	

	public void extractVariableBug() {
		String x= 2 + "3";
		System.out.println(1 + x);
	}
}