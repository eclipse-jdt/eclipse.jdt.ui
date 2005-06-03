package p;

public class TwoBoundedTypeParams_in {
	public void foo() {
		Complex<Integer,Integer> c1= Complex.createComplex(0, -1);
		Complex<Float,Float> c2= Complex.createComplex(0.0F, 3.14F);
	}
}
class Complex<TX extends Number, TY extends Number> {
	public static <TX extends Number, TY extends Number> Complex<TX, TY> createComplex(TX x, TY y) {
		return new Complex<TX, TY>(x, y);
	}
	TX fLeft;
	TY fRight;
	private /*[*/Complex/*]*/(TX x, TY y) {
		fLeft= x;
		fRight= y;
	}
}
