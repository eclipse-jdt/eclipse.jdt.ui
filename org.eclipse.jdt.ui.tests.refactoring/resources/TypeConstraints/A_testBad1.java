package p;
class A implements I{
	int NO= 0;

	void test(A a1, A a2){
		int k0= a1.YES;
		int k1= a2.NO;
	}
}
interface I{
	int YES= 0;
}