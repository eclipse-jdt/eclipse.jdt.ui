package p;
class A{
	boolean isFred(){return false;}
	int f(){
		boolean temp= new A().isFred();
		boolean i= temp;
		return 1;
	}
}