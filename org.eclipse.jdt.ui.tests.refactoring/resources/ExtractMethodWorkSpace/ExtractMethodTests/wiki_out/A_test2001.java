package wiki_out;

public class A_test2001 {

	int field= 0;

	void fun() {
		int i;
		extracted();
	}

	protected void extracted() {
		int i;
		/*[*/
		i= 0;
		System.out.println("i, field == " + i++ +", " + field);
		/*]*/
	}
}