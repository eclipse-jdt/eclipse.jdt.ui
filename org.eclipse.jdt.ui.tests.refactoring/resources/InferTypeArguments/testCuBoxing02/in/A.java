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
		c1.setT(17.3f);
		Number n= (Number) c1.getT();
		
		Cell c2= null;
		c2.setT(18);
		Cell c3= new Cell();
		c3.setT(new Long(23));
		c2= c3;
	}
}