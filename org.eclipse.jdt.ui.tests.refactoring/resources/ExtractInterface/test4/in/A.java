package p;

class A implements Cloneable, I{
	public void m(){
	}
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}