package p;

class A{
	class Inner{
	}
}
class I2 extends A.Inner{
	I2(){
		new A().super();
	}
}
class I3 extends I2{
}