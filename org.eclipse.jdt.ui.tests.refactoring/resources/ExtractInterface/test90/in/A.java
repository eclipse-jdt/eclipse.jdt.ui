package p;
class A {
	int x;
}
class ST{
}
class T extends ST{
	void add(A c){
		gm()[0]= c;
		
		gm1()[0]= c;
		gm1()[0].x= 0;
	}
	A[] gm() {
		return null;
	}
	A[] gm1() {
		return null;
	}

}