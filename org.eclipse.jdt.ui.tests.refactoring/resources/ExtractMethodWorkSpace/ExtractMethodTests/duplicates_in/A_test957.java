package duplicates_in;

public class A_test957 {
	public void f() {
		int i = 17;
		int k = 1;

		/*[*/i++;/*]*/
		k++;

		System.out.println(i);
		System.out.println(k);
	}
}
