package p;
class T {
	public String toString() {
		String temp= super.toString();
		return temp + new Integer(1).toString();	
	}
}
