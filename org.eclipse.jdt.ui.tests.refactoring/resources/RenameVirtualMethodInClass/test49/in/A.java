//can rename A.m() to k
package p;

interface ITest {
	public void m();
}
class A implements ITest {
    public void m() {}
    public static void main(String[] args) {
		new A().m();
	}
}