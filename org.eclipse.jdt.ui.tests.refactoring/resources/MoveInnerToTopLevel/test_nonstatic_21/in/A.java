package p;

class A{
	class Inner{
		Inner(){
		}
		Inner(int i){
		}
	}
}
class I2 extends A.Inner{
	I2(){
		new A().super();
	}
	I2(int i){
		new A().super(i);
	}
}