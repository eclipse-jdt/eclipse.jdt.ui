package p;
class A{
	void m(){
		int temp= 1 + 2;
		{
			int i= temp;
		}
		{
			int i= temp;
		}
	}
}