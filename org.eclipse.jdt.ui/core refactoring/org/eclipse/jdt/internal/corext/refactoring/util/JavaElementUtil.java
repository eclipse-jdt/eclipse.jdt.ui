package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.*;
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
		return JavaModelUtil.getFullyQualifiedName(field.getDeclaringType()) + "." + field.getElementName(); //$NON-NLS-1$
	}
	
	public static String createInitializerSignature(IInitializer initializer){
		String label= RefactoringCoreMessages.getString("JavaElementUtil.initializer_in") + JavaModelUtil.getFullyQualifiedName(initializer.getDeclaringType()); //$NON-NLS-1$
		try {
			if (JdtFlags.isStatic(initializer))
				return "static " + label; //$NON-NLS-1$
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
				return RefactoringCoreMessages.getString("JavaElementUtil.initializer"); //$NON-NLS-1$
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

	public static IType getMainType(ICompilationUnit cu) throws JavaModelException{
		IType[] types= cu.getTypes();
		for (int i = 0; i < types.length; i++) {
			if (isMainType(types[i]))
				return types[i];
		}
		return null;
	}
	
	public static boolean isMainType(IType type) throws JavaModelException{
		if (! type.exists())	
			return false;

		if (type.isBinary())
			return false;
			
		if (type.getCompilationUnit() == null)
			return false;
		
		if (type.getDeclaringType() != null)
			return false;
		
		return isPrimaryType(type) || isCuOnlyType(type);
	}


	private static boolean isPrimaryType(IType type){
		return type.getElementName().equals(Signature.getQualifier(type.getCompilationUnit().getElementName()));
	}


	private static boolean isCuOnlyType(IType type) throws JavaModelException{
		return (type.getCompilationUnit().getTypes().length == 1);
	}

	public static IMethod[] getAllConstructors(IType type) throws JavaModelException {
		if (type.isInterface())
			return new IMethod[0];
		List result= new ArrayList();
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			IMethod iMethod= methods[i];
			if (iMethod.isConstructor())
				result.add(iMethod);
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}
	
}
