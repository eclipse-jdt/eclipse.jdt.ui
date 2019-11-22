package p;

import java.io.IOException;

public interface IGeneric {

	<D> D genericMethod(String text) throws IOException;
}