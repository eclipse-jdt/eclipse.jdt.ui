package wiki_in;

public class A_test2004 {

	int field= 0;

	void fun() {
		int i= 0;
		while (field < 2) {
			field++;
			/*[*/
			System.out.println("i, field == " + i++ +", " + field);
			/*]*/
		}
	}
}