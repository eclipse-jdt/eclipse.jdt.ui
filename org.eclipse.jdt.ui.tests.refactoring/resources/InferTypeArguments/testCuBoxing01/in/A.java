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
		
		Cell c2= new Cell();
		c2.setT(new Short((short) 8));
		short s= (Short) c2.getT();
		
		Cell c3= new Cell();
		byte bite= 1;
		c3.setT(bite);
		bite= (Byte) c3.getT();
	}
}