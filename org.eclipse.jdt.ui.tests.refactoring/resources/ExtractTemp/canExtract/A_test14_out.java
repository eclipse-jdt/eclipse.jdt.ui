package p;

class A{
	int m(int y){
		int temp= 1 + 2;
		while(y==0)
			m(temp);
		return 1 + 2;
	}
}