package p;

class A extends Object implements Cloneable{
	public void m(){
	}
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}