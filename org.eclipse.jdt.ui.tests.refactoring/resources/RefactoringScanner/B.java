class B{
	/**
	 * TestTestPattern
	 * 	 TestPattern
	 */
	void f(){
	}
	
	/*
	 * TestTestPattern
	 * 
	 * 	TestPattern
	 */
	void f1(){
		f1();//TestPattern //org.eclipse.TestPattern
		String g= "TestPattern";//TestTestPattern
		String g2= "org.eclipse.TestPattern";
		String g3= "org.eclipse.TestPatternMatching";
	}
	
}