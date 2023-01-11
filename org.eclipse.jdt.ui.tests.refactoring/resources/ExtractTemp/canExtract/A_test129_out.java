package p; //5, 34, 5, 55

public class A {
	void foo(Object obj) {
		int hashCode= ((I7) obj).hashCode();
		if (obj instanceof CC && hashCode > 0) {
			System.out.println(hashCode);
		} 
	}
}
class CP implements I2,I5{}
class CR extends CP {}
class CC extends CR {}
interface I2 extends I3{}
interface I3 {}
interface I4 extends I5{}
interface I5 extends I6{}
interface I6 {}
interface I7 extends I6{}