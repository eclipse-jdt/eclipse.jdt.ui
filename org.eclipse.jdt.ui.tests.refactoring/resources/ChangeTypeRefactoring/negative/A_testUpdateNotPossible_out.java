import java.util.Hashtable;

class A_testUpdateNotPossible_in {
	public void foo {
		Hashtable h1 = new Hashtable();
		Hashtable h2 = new Hashtable();
		h1 = h2;
		h2 = h1;
	}
}
