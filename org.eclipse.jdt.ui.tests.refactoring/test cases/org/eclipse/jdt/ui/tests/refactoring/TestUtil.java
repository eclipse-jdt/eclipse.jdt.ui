package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;

class TestUtil {
	private TestUtil(){
	}
	
	static IMember[] merge(IMember[] a1, IMember[] a2){
		Set result= new HashSet(a1.length + a2.length);
		result.addAll(Arrays.asList(a1));
		result.addAll(Arrays.asList(a2));
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}
	
	static IField[] getFields(IType type, String[] names) throws JavaModelException{
	    if (names == null )
	        return new IField[0];
		Set fields= new HashSet();
		for (int i = 0; i < names.length; i++) {
			IField field= type.getField(names[i]);
			Assert.isTrue(field.exists(), "field " + field.getElementName() + " does not exist");
			fields.add(field);
		}
		return (IField[]) fields.toArray(new IField[fields.size()]);	
	}
	
	static IMethod[] getMethods(IType type, String[] names, String[][] signatures) throws JavaModelException{
		if (names == null || signatures == null)
			return new IMethod[0];
		Set methods= new HashSet();
		for (int i = 0; i < names.length; i++) {
			IMethod method= type.getMethod(names[i], signatures[i]);
			Assert.isTrue(method.exists(), "method " + method.getElementName() + " does not exist");
			methods.add(method);
		}
		return (IMethod[]) methods.toArray(new IMethod[methods.size()]);	
	}
	
}
