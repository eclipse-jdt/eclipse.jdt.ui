package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class JavaElementUtil {
	
	//no instances
	private JavaElementUtil(){
	}
	
	public static String createMethodSignature(IMethod method){
		try {
			return Signature.toString(method.getSignature(), method.getElementName(), method.getParameterNames(), false, true);
		} catch(JavaModelException e) {
			return method.getElementName(); //fallback
		}
	}
	
	public static String createFieldSignature(IField field){
		return field.getDeclaringType().getFullyQualifiedName() + "." + field.getElementName();
	}
	
	public static IJavaElement[] getElementsOfType(IJavaElement[] elements, int type){
		Set result= new HashSet(elements.length);
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];
			if (element.getElementType() == type)
				result.add(element);
		}
		return (IJavaElement[]) result.toArray(new IJavaElement[result.size()]);
	}
	
}
