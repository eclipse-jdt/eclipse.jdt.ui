package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class JavaElementUtil {
	
	//no instances
	private JavaElementUtil(){
	}
	
	public static String createMethodSignature(IMethod method) throws JavaModelException {
		return Signature.toString(method.getSignature(), method.getElementName(), method.getParameterNames(), false, true);
	}
}
