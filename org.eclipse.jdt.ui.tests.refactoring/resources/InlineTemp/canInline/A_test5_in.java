package p;
class A{
	int m(int i){
		int x= i + 1;
		return x * x + m(m(x));
	}
}