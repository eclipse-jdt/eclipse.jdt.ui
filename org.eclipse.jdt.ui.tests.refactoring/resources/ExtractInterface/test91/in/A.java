package p;
class A {
	int x;
}
class ST{
}
class T extends ST{
	static A[] scs;
	void add(A c){
		p.T.scs[0]= c;
		p.T.scs[0].x= 0;
	}
}