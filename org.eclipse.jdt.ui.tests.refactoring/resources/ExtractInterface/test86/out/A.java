package p;
class A implements I {
	int x;
}
class Y{
	A[] cs;
	void add(A c){
		cs[0]= c;
	}
	void f(){
		cs[0].x= 0;
	}
	void foo(){
		A[] tab= null;
		add(tab[0]);
	}
}