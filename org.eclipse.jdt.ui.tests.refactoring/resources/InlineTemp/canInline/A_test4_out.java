package p;
class A{
	int m(int i){
		return (i + 1) * (i + 1) + m(m(i));
	}
}