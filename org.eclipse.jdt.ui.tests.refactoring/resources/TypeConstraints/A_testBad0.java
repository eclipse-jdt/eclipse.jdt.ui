package p;
class A implements I{
	public void yes(){}
	public void no(){}

	void test(A a1, A a2){
		a1.yes();
		a2.no();
	}
}
interface I{
	public void yes();
}