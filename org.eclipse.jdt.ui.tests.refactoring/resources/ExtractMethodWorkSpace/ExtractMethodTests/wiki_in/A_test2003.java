package wiki_in;

public class A_test2003 {

	int field= 0;

	void fun() {
		int i= 0;
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