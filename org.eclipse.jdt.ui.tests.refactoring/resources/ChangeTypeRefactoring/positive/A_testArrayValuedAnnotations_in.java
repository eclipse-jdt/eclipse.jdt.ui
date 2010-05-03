public class A_testVarArg_in {
	@XSet(value = { @X })
	public String foo() { // generalize String
		return "";
	}
}

@interface XSet {
	public X[] value();
}

@interface X {
}
