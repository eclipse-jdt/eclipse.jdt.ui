package p;

public class TestInterfaceMethod2Impl implements ITestInterfaceMethod {

	public void foo(FooParameter parameterObject) {
		System.out.println(parameterObject.id+parameterObject.param+parameterObject.blubb);
	}

}
