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
		
		Cell<Short> c2= new Cell<Short>();
		c2.setT(new Short((short) 8));
		short s= c2.getT();
		
		Cell<Byte> c3= new Cell<Byte>();
		byte bite= 1;
		c3.setT(bite);
		bite= c3.getT();
	}
}