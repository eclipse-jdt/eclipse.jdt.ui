package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

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
		return JavaModelUtil.getFullyQualifiedName(field.getDeclaringType()) + "." + field.getElementName();
	}
	
	public static String createInitializerSignature(IInitializer initializer){
		String label= "initializer in " + JavaModelUtil.getFullyQualifiedName(initializer.getDeclaringType());
		try {
			if (JdtFlags.isStatic(initializer))
				return "static " + label;
			else 
				return label;
		} catch(JavaModelException e) {
			return label; //fallback
		}
	}
	
	public static String createSignature(IMember member){
		switch (member.getElementType()){
			case IJavaElement.FIELD:
				return createFieldSignature((IField)member);
			case IJavaElement.TYPE:
				return JavaModelUtil.getFullyQualifiedName(((IType)member));
			case IJavaElement.INITIALIZER:
				return "initializer";
			case IJavaElement.METHOD:
				return createMethodSignature((IMethod)member);				
			default:
				Assert.isTrue(false);
				return null;	
		}
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
