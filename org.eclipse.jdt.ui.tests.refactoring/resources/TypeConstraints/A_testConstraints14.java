package p;
class A{
	A f(A a0){
		return null;
	}
}
class B extends A implements I{
	A f(A a3){
		B ax= null;
		ax.f(a3);
		return null;
	}
}
interface I {
	A f(A ai);
}