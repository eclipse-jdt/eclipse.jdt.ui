package p;
class A{
	boolean isFred(){return false;}
	int f(){
		boolean i= isFred();
		isFred();
		return 1;
	}
}