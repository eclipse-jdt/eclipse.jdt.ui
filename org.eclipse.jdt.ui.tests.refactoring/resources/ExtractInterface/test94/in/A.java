package p;
class A {
	int x;
}
class ST{
	 A[] gm() {
		return null;
	}
}
class T extends ST{
	void add(A c){
		super.gm()[0]= c;
		super.gm()[0].x= 0;
	}
}