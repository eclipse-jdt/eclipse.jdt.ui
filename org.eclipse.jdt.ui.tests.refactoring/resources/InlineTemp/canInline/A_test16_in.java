package p;
class Test {
	void m(){
		Object object = Integer.valueOf(2);
		Integer integer = (Integer) object;
		int i = integer.intValue();
	}
}
