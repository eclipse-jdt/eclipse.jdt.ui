//selection: 7, 19, 7, 36
//name: string -> abc
package simple;

public class NewInstance1 {
	public void m(int a) {
		String s= new String("abc");
	}
}

class User {
	public void use() {
		new NewInstance1().m(17);
	}
}
