package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
	
	static IField[] findFields(IField[] fields, String[] namesOfFieldsToPullUp) {
		List found= new ArrayList(fields.length);
		for (int i= 0; i < fields.length; i++) {
			IField field= fields[i];
			for (int j= 0; j < namesOfFieldsToPullUp.length; j++) {
				String name= namesOfFieldsToPullUp[j];
				if (field.getElementName().equals(name))
					found.add(field);					
			}
		}
		return (IField[]) found.toArray(new IField[found.size()]);
	}

	static IMethod[] findMethods(IMethod[] selectedMethods, String[] namesOfMethods, String[][] signaturesOfMethods){
		List found= new ArrayList(selectedMethods.length);
		for (int i= 0; i < selectedMethods.length; i++) {
			IMethod method= selectedMethods[i];
			String[] paramTypes= method.getParameterTypes();
			for (int j= 0; j < namesOfMethods.length; j++) {
				String methodName= namesOfMethods[j];
				if (! methodName.equals(method.getElementName()))
					continue;
				String[] methodSig= signaturesOfMethods[j];
				if (! areSameSignatures(paramTypes, methodSig))
					continue;
				found.add(method);	
			}
		}
		return (IMethod[]) found.toArray(new IMethod[found.size()]);
	}
	
	static boolean areSameSignatures(String[] s1, String[] s2){
		if (s1.length != s2.length)
			return false;
		for (int i= 0; i < s1.length; i++) {
			if (! s1[i].equals(s2[i]))
				return false;
		}
		return true;
	}
}
