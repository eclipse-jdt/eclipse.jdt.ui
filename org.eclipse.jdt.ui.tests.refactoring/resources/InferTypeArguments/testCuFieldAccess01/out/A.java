package p;

class Cell<T> {
	T t;
	public void setT(T t) {
		this.t= t;
	}
	public T getT() {
		return t;
	}
}

class CellTest {
	public static void main(String[] args) {
		Cell<Integer> c1= new Cell<Integer>();
		c1.setT(17);
		Number n= c1.t;
		
		Cell<Boolean> c2= new Cell<Boolean>();
		c2.t= Boolean.TRUE;
		boolean b= c2.t;
		boolean b2= c2.getT();
	}
}
