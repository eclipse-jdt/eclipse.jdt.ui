package wiki_in;

public class A_test2001 {

	int field= 0;

	void fun() {
		int i;
		/*[*/
		i= 0;
		System.out.println("i, field == " + i++ +", " + field);
		/*]*/
	}
}