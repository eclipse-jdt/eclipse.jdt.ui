package p;

import java.io.IOException;
import java.util.function.Predicate;

class TestInlineLambda {
	static IOStream<String> main(IOStream<String> a) {
		// Trying to inline this variable causes an Internal Error:
		IOPredicate<String> allowed = word -> true;
		return a.filter(allowed);
	}
}

interface IOPredicate<T> {
    boolean test(T t1) throws IOException;
}

abstract class AbstractStream<T, SELF extends AbstractStream<T, SELF, PREDICATE>, PREDICATE> {
    SELF filter(PREDICATE allowed) {
        return null;
    }
    
    final @SafeVarargs SELF filter(Predicate<? super T> allow, Predicate<? super T>... allowed) {
        return null;
    }
}

class IOStream<T> extends AbstractStream<T, IOStream<T>, IOPredicate<? super T>> {
}
