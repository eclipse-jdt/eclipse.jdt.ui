package pack;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import junit.runner.Version;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class OrderedRunner extends BlockJUnit4ClassRunner {
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface Order {
        public String[] value();
    }
    
	public OrderedRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

	@Override
	protected List<FrameworkMethod> computeTestMethods() {
		List<FrameworkMethod> methods= super.computeTestMethods();
		
		Order order= getTestClass().getJavaClass().getAnnotation(Order.class);
		final List<String> orderedNames= Arrays.asList(order.value());
		
		Collections.sort(methods, new Comparator<FrameworkMethod>() {
			public int compare(FrameworkMethod m1, FrameworkMethod m2) {
				String n1= m1.getName();
				String n2= m2.getName();
				
				int i1= orderedNames.indexOf(n1);
				int i2= orderedNames.indexOf(n2);
				
				return i1 < i2 ? -1 : 1;
			}
		});
		
		return methods;
	}
}