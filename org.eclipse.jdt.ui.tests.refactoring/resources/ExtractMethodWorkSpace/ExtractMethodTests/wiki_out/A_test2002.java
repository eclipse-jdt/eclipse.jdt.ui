package wiki_out;

public class A_test2002 {

	int field= 0;
	void fun() {
		int i= 0;
		i = extracted(i);
		System.out.println("i == " + i);
	}
	protected int extracted(int i) {
		/*[*/
		System.out.println("i, field == " + i++ +", " + field);
		/*]*/
		return i;
	}
}