package p;
class A{
	boolean isFred(){return false;}
	int f(){
		boolean i= new A().isFred();
		return 1;
	}
}