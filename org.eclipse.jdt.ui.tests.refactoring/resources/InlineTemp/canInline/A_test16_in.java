package p;
class Test {
	void m(){
		Object object = new Integer(2);
		Integer integer = (Integer) object;
		int i = integer.intValue();
	}
}
