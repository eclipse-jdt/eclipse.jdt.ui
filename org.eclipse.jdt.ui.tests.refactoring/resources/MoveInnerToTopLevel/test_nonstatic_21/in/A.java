package p;

class A{
	class Inner{
		Inner(){
		}
		Inner(int i){
		}
	}
}
class I2 extends Inner{
	I2(){
		super();
	}
	I2(int i){
		super(i);
	}
}