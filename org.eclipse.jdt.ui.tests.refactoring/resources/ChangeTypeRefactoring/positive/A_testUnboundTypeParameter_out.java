import java.util.List;

public class A_testUnboundTypeParameter_in {
	public <T> void baz() {
		Iterable<T> list= null;
	}
}
