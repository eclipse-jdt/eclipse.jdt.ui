package p1;
import p.Inner;
public class A1 {
	static void f(){
		Inner i;
		Inner.foo();
		Inner.t =  2;
		p.Inner.foo();
		p.Inner.t =  2;
	}

}