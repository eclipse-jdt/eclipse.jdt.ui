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
		Cell c1= new Cell();
		c1.setT(17);
		Number n= (Number) c1.t;
		
		Cell c2= new Cell();
		c2.t= Boolean.TRUE;
		boolean b= (Boolean) c2.t;
		boolean b2= (Boolean) c2.getT();
	}
}
