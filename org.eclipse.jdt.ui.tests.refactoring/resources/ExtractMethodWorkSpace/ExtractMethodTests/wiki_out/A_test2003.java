package wiki_out;

public class A_test2003 {

	int field= 0;

	void fun() {
		int i= 0;
		extracted(i);
	}

	protected void extracted(int i) {
		/*[*/
		if (field == 1) {
			i= 1;
			System.out.println("i, field == " + i + ", " + field);
		} else {
			System.out.println("i, field == " + i + ", " + field);
		}
		/*]*/
	}
}