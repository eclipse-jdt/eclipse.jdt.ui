package p;

class A{
}
class I2 extends A.Inner{
	I2(){
		super(new A());
	}
}
class I3 extends I2{
}