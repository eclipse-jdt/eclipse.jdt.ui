package p;
class A{
	int getLength(){return 0;}
	int f(){
		int temp= new A().getLength();
		int i= temp;
		return 1;
	}
}