package p;

public class B implements OldInterface{
	private void s(){
		OldInterface i= find();
	}
	private A find(){
		return new A();
	}
}
