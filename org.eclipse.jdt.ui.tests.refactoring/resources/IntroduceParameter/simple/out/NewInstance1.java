//selection: 7, 19, 7, 36
//name: string -> abc
package simple.out;

public class NewInstance1 {
	public void m(int a, String abc) {
		String s= abc;
	}
}

class User {
	public void use() {
		new NewInstance1().m(17, new String("abc"));
	}
}
