package wiki_in;

public class A_test2002 {

	int field= 0;
	void fun() {
		int i= 0;
		/*[*/
		System.out.println("i, field == " + i++ +", " + field);
		/*]*/
		System.out.println("i == " + i);
	}
}