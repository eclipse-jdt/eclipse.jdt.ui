class Example {
	public static final Example A = new Example("A2", "A1");
	public static final Example B = Example.getExample("B1", "B2");
	
	public static final Example C;    
	public static final Example D;
	static {
		C = new Example("C2", "C1");
		D = Example.getExample("D1", "D2");
	}
	
	public Example(String b, String a) {

	}
	
	public static Example getExample(String arg1, String arg2) {
		return new Example(arg2, arg1);
	}
}
