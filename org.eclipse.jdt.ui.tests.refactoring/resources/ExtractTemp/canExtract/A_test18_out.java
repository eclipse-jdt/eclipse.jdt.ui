package p;
class A{
	int m(int y){
		m(0);
		int temp= 1 + 2;
		{
			int x= temp;
		}
		{
			{
				int x= temp;
			}
		}
		return temp;
	}
}