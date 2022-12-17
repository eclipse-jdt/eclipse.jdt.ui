package p; //5, 34, 5, 55

public class A {
	void foo(Object obj) {
		if (obj instanceof I2 && ((I6) obj).hashCode() > 0) {
			int hashCode= ((I6) obj).hashCode();
			System.out.println(hashCode);
		} 
	}
}
interface I2 extends I3,I4{}
interface I3 {}
interface I4 extends I5{}
interface I5 extends I6{}
interface I6 {}
interface I7 extends I6{}