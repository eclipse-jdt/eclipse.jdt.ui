package p;
public class Bar {

  private IFoo foo;
  
  public IFoo getFoo() {
	return foo;
  }

  public void setFoo(IFoo foo) {
	this.foo = foo;
  }

  public void useFoo() {
	foo.foo();
  }
}
