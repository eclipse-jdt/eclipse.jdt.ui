package p;
class T {
	public String toString() {
		String temp= super.toString();
		return temp + Integer.valueOf(1).toString();	
	}
}
