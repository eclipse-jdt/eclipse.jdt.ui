class Example {
	public static final Example A = new Example("A1", "A2");
	public static final Example B = Example.getExample("B1", "B2");
	
	public static final Example C;    
	public static final Example D;
	static {
		C = new Example("C1", "C2");
		D = Example.getExample("D1", "D2");
	}
	
	public Example(String arg1, String arg2) {

	}
	
	public static Example getExample(String arg1, String arg2) {
		return new Example(arg1, arg2);
	}
}
