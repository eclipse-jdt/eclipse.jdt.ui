package p;
class A{
	boolean isFred(){return false;}
	int f(){
		boolean temp= isFred();
		boolean i= temp;
		isFred();
		return 1;
	}
}