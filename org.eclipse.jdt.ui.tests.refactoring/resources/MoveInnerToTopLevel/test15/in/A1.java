package p1;
import p.A;
public class A1 {
	static void f(){
		A.Inner i;
		A.Inner.foo();
		A.Inner.t =  2;
		p.A.Inner.foo();
		p.A.Inner.t =  2;
	}

}