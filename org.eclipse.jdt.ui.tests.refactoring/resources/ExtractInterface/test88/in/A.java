package p;
class A {
	int x;
}
class ST{
}
class T extends ST{
	A[] cs;
	void add(A c){
		(cs= cs)[0]= c;
		cs[0].x= 0;
	}
}