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
		Cell<Number> c1= new Cell<Number>();
		c1.setT(17);
		c1.setT(17.3f);
		Number n= c1.getT();
		
		Cell<Number> c2= null;
		c2.setT(18);
		Cell<Number> c3= new Cell<Number>();
		c3.setT(new Long(23));
		c2= c3;
	}
}