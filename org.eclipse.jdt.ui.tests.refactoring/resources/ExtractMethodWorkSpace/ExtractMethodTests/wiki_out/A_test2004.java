package wiki_out;

public class A_test2004 {

	int field= 0;

	void fun() {
		int i= 0;
		while (field < 2) {
			field++;
			i = extracted(i);
		}
	}

	protected int extracted(int i) {
		/*[*/
		System.out.println("i, field == " + i++ +", " + field);
		/*]*/
		return i;
	}
}