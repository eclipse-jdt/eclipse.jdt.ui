//selection: 9, 32, 9, 37
//name: j -> foo
package simple;

public class StaticGetter1 {
	public static int foo() { return 17; }
	void bar() {
		int i= 3;
		System.out.println(i + foo());
	}
	void use() {
		bar();
	}
}
