/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

/**
 *
 */
public class ASTChildConstantsGenerator extends TestCase {
	
	private static final String LIST= "java.util.List";
	
	public static TypeFilter fgListFilter= new NodeTypeFilter(List.class);
	public static TypeFilter fgASTFilter= new NodeTypeFilter(ASTNode.class);
	public static TypeFilter fgPrimitiveFilter= new PrimitiveTypeFilter();
	public static TypeFilter fgNoFilter= new TypeFilter();
	
	public static class TypeFilter {
		public boolean accept(Class returnType) {
			return true;
		}
	}
	
	public static class NodeTypeFilter extends TypeFilter {
		private Class fClass;

		public NodeTypeFilter(Class cl) {
			fClass= cl;
		}
		
		public boolean accept(Class returnType) {
			return fClass.isAssignableFrom(returnType);
		}
	}
	
	public static class PrimitiveTypeFilter extends TypeFilter {
		public PrimitiveTypeFilter() {
		}
		
		public boolean accept(Class returnType) {
			return returnType.isPrimitive();
		}
	}	
	
	
	public class ClassNameComparer implements Comparator {

		private Collator fCollator= Collator.getInstance();
		
		public int compare(Object o1, Object o2) {
			String name1= ((Class)o1).getName();
			String name2= ((Class)o2).getName();		
			return fCollator.compare(name1, name2);
		}

	}
	
	public class MethodNameComparer implements Comparator {

		private Collator fCollator= Collator.getInstance();
		
		public int compare(Object o1, Object o2) {
			String name1= ((Method)o1).getName();
			String name2= ((Method)o2).getName();		
			return fCollator.compare(name1, name2);
		}

	}	
	private static final Class THIS= ASTChildConstantsGenerator.class;

	private IJavaProject fJProject1;

	private static HashSet fgPropertyIgnoreList;
	
	static {
		fgPropertyIgnoreList= new HashSet();
		fgPropertyIgnoreList.add("setProperty");
		fgPropertyIgnoreList.add("setParent");
		fgPropertyIgnoreList.add("setFlags");
		fgPropertyIgnoreList.add("setLeadingComment");
	}
	

	public ASTChildConstantsGenerator(String name) {
		super(name);
		

		
	}

	public static Test allTests() {
		return new TestSuite(THIS);
	}

	public static Test suite() {
		if (false) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ASTChildConstantsGenerator("createGeneratedCU"));
			return suite;
		}	
	}

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	
	
	public void createGeneratedCU() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package org.eclipse.jdt.internal.corext.dom;\n\n");
		buf.append("import java.util.List;");
		buf.append("import org.eclipse.jdt.core.dom.*;\n\n");
		buf.append("public class ASTNodeConstants {\n");
		
		createPropertyList(buf);
		createPropertyValidator(buf);
		createIsListTest(buf);
		createListAccessor(buf);
		createNodeAccessor(buf);
		createPrimitiveAttibutesAccessor(buf);
		
		buf.append("}");
		
		String code= buf.toString();
		
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.FORMATTER_LINE_SPLIT, "999");
		
		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, code, 0, String.valueOf('\n'), options);
		String res= CodeFormatterUtil.evaluateFormatterEdit(code, edit, null);
		System.out.print(res);
	}
	


	public void createPropertyList(StringBuffer buf) throws Exception {
		HashMap properties= getPropertyList();
		
		Object[] objects= properties.keySet().toArray();
		Arrays.sort(objects, Collator.getInstance());
		for (int i= 0; i < objects.length; i++) {
			String curr= (String) objects[i];
			
			buf.append("/**\n");
			buf.append(" * Property for nodes of type\n");
			List parents= (List) properties.get(curr);
			Collections.sort(parents, new ClassNameComparer());
			int lastParent= parents.size() - 1;
			for (int k= 0; k <= lastParent; k++) {
				Class parent= (Class) parents.get(k);
				buf.append(" * <code>" + Signature.getSimpleName(parent.getName()) + "</code>");
				if (k != lastParent) {
					buf.append(",\n");
				} else {
					buf.append(".\n");
				}
			}
			buf.append(" */\n");
			buf.append("public static final int " + curr + "= " + String.valueOf(i + 1) + ";\n\n");
		}
	}

	public void createPropertyValidator(StringBuffer buf) throws Exception {
		HashMap classes= createClassList();
		
		buf.append("public static boolean hasChildPropery(ASTNode node, int property) {");	
		buf.append("switch (node.getNodeType()) {");	
		
		Object[] objects= classes.keySet().toArray();
		Arrays.sort(objects, new ClassNameComparer());
		for (int i= 0; i < objects.length; i++) {
			Class curr= (Class) objects[i];
			Object[] properties= ((List) classes.get(curr)).toArray();
			Arrays.sort(properties, Collator.getInstance());
			
			int nProperties= properties.length;
			if (nProperties != 0) {
				String name= Signature.getSimpleName(curr.getName());
				buf.append("case ASTNode." + toConstantName(name) + ":");
				if (nProperties == 1) {
					buf.append("return property == " +properties[0] + ";");
				} else if (nProperties == 2) {
					buf.append("return (property == " +properties[0] + ") || (property == " +properties[1] + ");");
				} else if (nProperties == 3) {
					buf.append("return (property == " +properties[0] + ") || (property == " +properties[1] + ") || (property == " +properties[2] + ");");
				} else {
					buf.append("switch (property) {");
					for (int k= 0; k < nProperties; k++) {
						buf.append("case " + properties[k] + ":");
					}
					buf.append("return true;");
					buf.append("}");
					buf.append("return false;");
				}				
			}
		}
		buf.append("}");	
		buf.append("return false;");
		buf.append("}");
	}
	
	public void createIsListTest(StringBuffer buf) throws Exception {
		
		HashSet listProperties= new HashSet();
		HashSet otherProperties= new HashSet();
		
		Class[] astClases= getASTClasses();
		
		for (int i= 0; i < astClases.length; i++) {
			Class cl= astClases[i];
			Method[] properties= getProperties(cl);
			for (int k= 0; k < properties.length; k++) {
				Method curr= properties[k];
				String name= toConstantName(getPropertyName(curr));
				if (!LIST.equals(curr.getReturnType().getName())) {
					otherProperties.add(name);
					if (listProperties.contains(name)) {
						System.out.println("Property is list and non-list: " + name);
					}
				} else {
					listProperties.add(name);
					if (otherProperties.contains(name)) {
						System.out.println("Property is list and non-list: " + name);
					}					
				}
			}
		}

		String[] lists= (String[]) listProperties.toArray(new String[listProperties.size()]);
		Arrays.sort(lists, Collator.getInstance());
		
		buf.append("public static boolean isListPropery(int property) {");	
		buf.append("switch (property) {");	

		for (int i= 0; i < lists.length; i++) {
			buf.append("case " + lists[i] + ":");
		}
		buf.append("return true;");
		buf.append("}");	
		buf.append("return false;");
		buf.append("}");
	}
	
	
	public void createListAccessor(StringBuffer buf) throws Exception {
		Class[] astClases= getASTClasses();
		Arrays.sort(astClases, new ClassNameComparer());

		buf.append("public static List getListPropery(ASTNode node, int property) {");	
		buf.append("switch (node.getNodeType()) {");	

		for (int i= 0; i < astClases.length; i++) {
			Class curr= astClases[i];
			Method[] properties= getProperties(curr, fgListFilter);
			
			int nProperties= properties.length;
			if (nProperties != 0) {
				String className= Signature.getSimpleName(curr.getName());
				buf.append("case ASTNode." + toConstantName(className) + ":");

				if (nProperties == 1) {
					String methodName= properties[0].getName();
					buf.append("if (property == " + toConstantName(methodName) + ") {");
					buf.append("return ((" + className + ") node)." + methodName + "();");
					buf.append("}");
					buf.append("break;");
				} else {
					Arrays.sort(properties, new MethodNameComparer());
					
					buf.append("switch (property) {");
					for (int k= 0; k < nProperties; k++) {
						String methodName= properties[k].getName();
						
						buf.append("case " + toConstantName(methodName) + ":");
						buf.append("return ((" + className + ") node)." + methodName + "();");
					}
					buf.append("}");
					buf.append("break;");
				}				
			}
		}
		buf.append("}");	
		buf.append("throw new IllegalArgumentException();");
		buf.append("}");
	}
	
	public void createNodeAccessor(StringBuffer buf) throws Exception {
		Class[] astClases= getASTClasses();
		Arrays.sort(astClases, new ClassNameComparer());

		buf.append("public static ASTNode getNodePropery(ASTNode node, int property) {");	
		buf.append("switch (node.getNodeType()) {");	

		for (int i= 0; i < astClases.length; i++) {
			Class curr= astClases[i];
			Method[] properties= getProperties(curr, fgASTFilter);
			
			int nProperties= properties.length;
			if (nProperties != 0) {
				String className= Signature.getSimpleName(curr.getName());
				buf.append("case ASTNode." + toConstantName(className) + ":");

				if (nProperties == 1) {
					String name= getPropertyName(properties[0]);
					buf.append("if (property == " + toConstantName(name) + ") {");
					buf.append("return ((" + className + ") node)." + properties[0].getName() + "();");
					buf.append("}");
					buf.append("break;");
				} else {
					Arrays.sort(properties, new MethodNameComparer());
					
					buf.append("switch (property) {");
					for (int k= 0; k < nProperties; k++) {
						String name= getPropertyName(properties[k]);
						
						buf.append("case " + toConstantName(name) + ":");
						buf.append("return ((" + className + ") node)." + properties[k].getName() + "();");
					}
					buf.append("}");
					buf.append("break;");
				}				
			}
		}
		buf.append("}");	
		buf.append("throw new IllegalArgumentException();");
		buf.append("}");
	}
	
	public void createPrimitiveAttibutesAccessor(StringBuffer buf) throws Exception {
		Class[] astClases= getASTClasses();
		Arrays.sort(astClases, new ClassNameComparer());

		buf.append("public static Object getAttributePropery(ASTNode node, int property) {");	
		buf.append("switch (node.getNodeType()) {");	

		for (int i= 0; i < astClases.length; i++) {
			Class curr= astClases[i];
			Method[] properties= getProperties(curr, fgPrimitiveFilter);
			
			int nProperties= properties.length;
			if (nProperties != 0) {
				String className= Signature.getSimpleName(curr.getName());
				buf.append("case ASTNode." + toConstantName(className) + ":");

				if (nProperties == 1) {
					String name= getPropertyName(properties[0]);
	
					buf.append("if (property == " + toConstantName(name) + ") {");
					buf.append("return ");
					if ("int".equals(properties[0].getReturnType().getName())) {
						buf.append("new Integer(((");
					} else {
						buf.append("new Boolean(((");
					}
					buf.append(className + ") node)." + properties[0].getName() + "());");
					buf.append("}");
					buf.append("break;");
				} else {
					Arrays.sort(properties, new MethodNameComparer());
					
					buf.append("switch (property) {");
					for (int k= 0; k < nProperties; k++) {
						String name= getPropertyName(properties[k]);
						
						buf.append("case " + toConstantName(name) + ":");
						buf.append("return ");
						if ("int".equals(properties[k].getReturnType().getName())) {
							buf.append("new Integer(((");
						} else {
							buf.append("new Boolean(((");
						}
						buf.append(className + ") node)." + properties[k].getName() + "());");		
					}
					buf.append("}");
					buf.append("break;");
				}				
			}
		}
		buf.append("}");	
		buf.append("throw new IllegalArgumentException();");
		buf.append("}");
	}	
	
	
	private HashMap getPropertyList() {
		Class[] astClases= getASTClasses();
		
		HashMap result= new HashMap();
		for (int i= 0; i < astClases.length; i++) {
			Class cl= astClases[i];
			Method[] properties= getProperties(cl);
			for (int k= 0; k < properties.length; k++) {
				Method curr= properties[k];
				String name= getPropertyName(curr);
				addToListMap(result, toConstantName(name), cl);
			}
		}
		return result;
	}
	
	private HashMap createClassList() {
		Class[] astClases= getASTClasses();
		
		HashMap result= new HashMap();
		for (int i= 0; i < astClases.length; i++) {
			Class cl= astClases[i];
			Method[] properties= getProperties(cl);
			for (int k= 0; k < properties.length; k++) {
				Method curr= properties[k];
				String name= getPropertyName(curr);
				addToListMap(result, cl, toConstantName(name));
			}
		}
		return result;
	}	
	
	private static String getPropertyName(Method curr) {
		String name= curr.getName();
		if (name.startsWith("get")) {
			return name.substring(3);
		} else if (name.startsWith("is")) {
			return name;
		}
		return name;
	}
	
	
	private void addToListMap(HashMap properties, Object key, Object value) {
		ArrayList list= (ArrayList) properties.get(key);
		if (list == null) {
			list= new ArrayList(3);
			properties.put(key, list);
		}
		list.add(value);
	}
	

	private static Class[] getASTClasses() {
		ArrayList list= new ArrayList();
		Class class1= ASTVisitor.class;
		Method[] methods= class1.getMethods();
		for (int i= 0; i < methods.length; i++) {
			Method curr= methods[i];
			if ("visit".equals(curr.getName())) {
				Class param= curr.getParameterTypes()[0];
				list.add(param);
			}
		}
		return (Class[]) list.toArray(new Class[list.size()]);
	}
		
	private static Method[] getProperties(Class param) {
		return getProperties(param, fgNoFilter);
	}
	
	private static Method[] getProperties(Class param, TypeFilter filter) {
		ArrayList res= new ArrayList();
		Method[] methods= param.getMethods();
		for (int i= 0; i < methods.length; i++) {
			Method curr= methods[i];
			int modifiers= curr.getModifiers();
			if ((modifiers & (Modifier.STATIC | Modifier.PUBLIC)) == Modifier.PUBLIC) {
				String name= curr.getName();
				if (!fgPropertyIgnoreList.contains(name)) {
					Class returnType= curr.getReturnType();
					if (returnType.isArray()) {
						continue;
					}
					
					if (LIST.equals(returnType.getName())) {
						if (filter.accept(returnType) ) {
							res.add(curr);
						}
					} else if (name.startsWith("set")) {
						Method getter= findGetter(methods, name.substring(3));
						if (getter != null && filter.accept(getter.getReturnType())) {
							res.add(getter);
						}
					}
				}
			}
		}
		return (Method[]) res.toArray(new Method[res.size()]);
	}
	
	private static Method findGetter(Method[] methods, String string) {	
		String s1= "get" + string;
		String s2= "is" + string;
		for (int i= 0; i < methods.length; i++) {
			String name= methods[i].getName();
			if (s1.equals(name) || s2.equals(name)) {
				return methods[i];
			}
		}
		return null;
	}

	private static String toConstantName(String string) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < string.length(); i++) {
			char ch= string.charAt(i);
			if (i != 0 && Character.isUpperCase(ch)) {
				buf.append('_');
			}
			buf.append(Character.toUpperCase(ch));
		}
		return buf.toString();
	}
	
}