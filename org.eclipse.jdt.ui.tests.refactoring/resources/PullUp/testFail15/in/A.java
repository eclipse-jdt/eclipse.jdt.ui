package p;
class A{}
class C1 extends A{
	public String toString(){
		return null;
	}
}
class B extends C1 {
	public void m() {
		super.toString();
	}
}

