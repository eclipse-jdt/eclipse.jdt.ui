//can rename A.m() to k
package p;

interface ITest {
	public void k();
}
class A implements ITest {
    public void k() {}
    public static void main(String[] args) {
		new A().k();
	}
}