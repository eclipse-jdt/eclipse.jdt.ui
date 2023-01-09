package p; //5, 34, 5, 55

public class A {
	void foo(Object obj) {
		if (obj instanceof CC && ((I6) obj).hashCode() > 0) {
			int hashCode= ((I6) obj).hashCode();
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