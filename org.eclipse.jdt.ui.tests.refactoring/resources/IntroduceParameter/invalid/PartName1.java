//selection: 7, 17, 7, 19
package invalid;

class PartName1 {
	public static int foo() { return 17; }
	void bar() {
		int a= foo();
	}
	void use() {
		bar();
	}
}
