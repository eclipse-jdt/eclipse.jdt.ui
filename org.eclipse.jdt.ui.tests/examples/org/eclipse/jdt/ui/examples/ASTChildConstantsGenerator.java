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

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

/**
 *
 */
public class ASTChildConstantsGenerator extends TestCase {
	
	private static final String LIST= "java.util.List";
	
	public static TypeFilter fgListFilter= new NodeTypeFilter(List.class);
	public static TypeFilter fgASTFilter= new NodeTypeFilter(ASTNode.class);
	public static TypeFilter fgListAndASTFilter=  new PrimitiveTypeFilter(true);
	public static TypeFilter fgPrimitiveFilter= new PrimitiveTypeFilter(false);
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

		private boolean fInvert;

		public PrimitiveTypeFilter(boolean b) {
			fInvert= b;
		}
		
		public boolean accept(Class returnType) {
			return fInvert ^ (!LIST.equals(returnType.getName()) && !ASTNode.class.isAssignableFrom(returnType));
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
	
	public class MethodTypeComparer implements Comparator {

		private Collator fCollator= Collator.getInstance();
		
		public int compare(Object o1, Object o2) {
			Method m1= (Method)o1;
			Method m2= (Method)o2;
			boolean isList1= LIST.equals(m1.getReturnType().getName());
			boolean isList2= LIST.equals(m2.getReturnType().getName());
			if (isList1 && !isList2) {
				return 1;
			} else if (!isList1 && isList2) {
				return -1;
			}
			String name1= m1.getName();
			String name2= m2.getName();		
			return fCollator.compare(name1, name2);
		}

	}	
	
	private static final Class THIS= ASTChildConstantsGenerator.class;

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
	}


	protected void tearDown() throws Exception {
	}
	
	
	public void createGeneratedCU() throws Exception {
		
		Class[] astClases= getASTClasses();
		Arrays.sort(astClases, new ClassNameComparer());
		
		StringBuffer buf= new StringBuffer();
		buf.append("package org.eclipse.jdt.internal.corext.dom;\n\n");
		buf.append("import java.util.List;");
		buf.append("import org.eclipse.jdt.core.dom.*;\n\n");
		buf.append("public class ASTNodeConstants {\n");
		
		createPropertyList(astClases, buf);
		createPropertyValidator(astClases, buf);
		createIsListTest(astClases, buf);
		createListAccessor(astClases, buf);
		createGetNodeChild(astClases, buf);
		createGetPropertyOfNode(astClases, buf);
		createGetNodeChildProperties(astClases, buf);
		
		buf.append("}");
		
		String code= buf.toString();
		
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.FORMATTER_LINE_SPLIT, "999");
		
		// new formatter
		//TextEdit edit= ToolFactory.createCodeFormatter(options).format(CodeFormatter.K_COMPILATION_UNIT, code, 0, code.length(), 0, String.valueOf('\n'));
		//Document doc= new Document(code);
		//edit.apply(doc, 0);
		//String res= doc.get();
		
		// old formatter
		String res= ToolFactory.createDefaultCodeFormatter(options).format(code, 0, null, String.valueOf('\n'));
		System.out.print(res);
	}
	


	public void createPropertyList(Class[] astClases, StringBuffer buf) throws Exception {
		HashMap properties= getPropertyList(astClases);
		
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

	public void createPropertyValidator(Class[] astClases, StringBuffer buf) throws Exception {
		HashMap classes= createClassList(astClases);
		
		buf.append("\n/**");	
		buf.append("\n *Returns <code>true</code> if a node has the given property.");
		buf.append("\n */\n");
		buf.append("public static boolean hasChildProperty(ASTNode node, int property) {");	
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
		buf.append("}\n\n");
	}
	
	public void createIsListTest(Class[] astClases, StringBuffer buf) throws Exception {
		
		HashSet listProperties= new HashSet();
		HashSet otherProperties= new HashSet();
				
		for (int i= 0; i < astClases.length; i++) {
			Class cl= astClases[i];
			Method[] properties= getProperties(cl);
			for (int k= 0; k < properties.length; k++) {
				Method curr= properties[k];
				Class ret= curr.getReturnType();

				String name= toConstantName(getPropertyName(curr));
				if (!LIST.equals(ret.getName())) {
					otherProperties.add(name);
					if (listProperties.contains(name)) {
						System.err.println("Property is list and non-list: " + name);
					}
					if (!fgASTFilter.accept(ret) && !fgPrimitiveFilter.accept(ret)) {
						System.err.println("Property has unknown return type: " + ret.getName());
					}
				} else {
					if (otherProperties.contains(name)) {
						System.err.println("Property is list and non-list: " + name);
					} else {
						listProperties.add(name);
					}
				}
			}
		}

		String[] lists= (String[]) listProperties.toArray(new String[listProperties.size()]);
		Arrays.sort(lists, Collator.getInstance());
		
		buf.append("\n/**");	
		buf.append("\n  * Returns <code>true</code> if property of a node is a list property.");
		buf.append("\n */\n");
		buf.append("public static boolean isListProperty(int property) {");	
		buf.append("switch (property) {");	
	
		for (int i= 0; i < lists.length; i++) {
			buf.append("case " + lists[i] + ":");
		}
		buf.append("return true;");
		buf.append("}");	
		buf.append("return false;");
		buf.append("}\n\n");
	}
	
	
	public void createListAccessor(Class[] astClases, StringBuffer buf) throws Exception {
		
		buf.append("\n/**");	
		buf.append("\n * Gets a property in a list.");
		buf.append("\n */\n");
		buf.append("public static ASTNode getNodeChild(ASTNode node, int property, int index) {");	
		buf.append("if (!isListProperty(property)) {");
		buf.append("throw new IllegalArgumentException();");
		buf.append("}");
		buf.append("return (ASTNode) ((List) getNodeChild(node, property)).get(index);");
		buf.append("}\n\n");
		
	}
	
	

	
	public void createGetNodeChild(Class[] astClases, StringBuffer buf) throws Exception {


		buf.append("\n/**");	
		buf.append("\n * Gets a ASTNode child by the property id. Booleans and integer attributes are returned boxed.");
		buf.append("\n */\n");
		buf.append("public static Object getNodeChild(ASTNode node, int property) {");	
		buf.append("switch (node.getNodeType()) {");	

		for (int i= 0; i < astClases.length; i++) {
			Class curr= astClases[i];
			Method[] properties= getProperties(curr, fgNoFilter);
			
			int nProperties= properties.length;
			if (nProperties != 0) {
				String className= Signature.getSimpleName(curr.getName());
				buf.append("case ASTNode." + toConstantName(className) + ":");

				if (nProperties == 1) {
					String name= getPropertyName(properties[0]);
	
					buf.append("if (property == " + toConstantName(name) + ") {");
					appendWrapped(buf, className, properties[0]);		
					buf.append("}");
					buf.append("break;");
				} else {
					Arrays.sort(properties, new MethodNameComparer());
					
					buf.append("switch (property) {");
					for (int k= 0; k < nProperties; k++) {
						
						Method meth= properties[k];
						String name= getPropertyName(meth);
						buf.append("case " + toConstantName(name) + ":");
						appendWrapped(buf, className, meth);		
					}
					buf.append("}");
					buf.append("break;");
				}				
			}
		}
		buf.append("}");	
		buf.append("throw new IllegalArgumentException();");
		buf.append("}\n\n");
	}
	
	
	
	
	private void appendWrapped(StringBuffer buf, String className, Method meth) {
		buf.append("return ");
		String val= "((" + className + ") node)." + meth.getName() + "()";
		
		String retType= meth.getReturnType().getName();
		if ("int".equals(retType)) {
			buf.append("new Integer(");
			buf.append(val);
			buf.append(")");
		} else if ("boolean".equals(retType)){
			buf.append("new Boolean(");
			buf.append(val);
			buf.append(")");
		} else {
			buf.append(val);
		}
		buf.append(";");
	}

	public void createGetPropertyOfNode(Class[] astClases, StringBuffer buf) throws Exception {

		buf.append("\n/**");	
		buf.append("\n * Gets the child property a node is located at its parent.");
		buf.append("\n */\n");
		buf.append("public static int getPropertyOfNode(ASTNode node) {");
		buf.append("ASTNode parent=node.getParent();");
		buf.append("if (parent == null) {");
		buf.append("throw new IllegalArgumentException();");
		buf.append("}");
		
		buf.append("switch (parent.getNodeType()) {");

		MethodTypeComparer comparer= new MethodTypeComparer();
		
		for (int i= 0; i < astClases.length; i++) {
			Class curr= astClases[i];
			Method[] properties= getProperties(curr, fgListAndASTFilter);
			
			int nProperties= properties.length;
			if (nProperties != 0) {
				String className= Signature.getSimpleName(curr.getName());
				buf.append("case ASTNode." + toConstantName(className) + ":");
				String varName= "((" + className + ") parent)";
				if (nProperties > 2) {
					varName= toVariableName(className);
					buf.append(className);
					buf.append(" ");
					buf.append(varName);
					buf.append("=(");
					buf.append(className);
					buf.append(")parent;");
				}
				Arrays.sort(properties, comparer);
				for (int k= 0; k < properties.length; k++) {
					Method meth= properties[k];
					if (k != properties.length - 1) {
						buf.append("if (").append(varName).append(".");
						buf.append(meth.getName());
						if (LIST.equals(meth.getReturnType().getName())) {
							buf.append("().contains(node))"); 
						} else {
							buf.append("() == node)"); 
						}
					}
					buf.append("return ");
					buf.append(toConstantName(getPropertyName(meth)));
					buf.append(";");
				}
			}
		}
		buf.append("}");	
		buf.append("throw new IllegalArgumentException();");
		buf.append("}\n\n");
	}
	
	public void createGetNodeChildProperties(Class[] astClases, StringBuffer buf) throws Exception {

		buf.append("\n/**");	
		buf.append("\n * Returns the properties of all children.");
		buf.append("\n */\n");
		buf.append("public static int[] getNodeChildProperties(ASTNode node) {");
		
		buf.append("switch (node.getNodeType()) {");
		
		for (int i= 0; i < astClases.length; i++) {
			Class curr= astClases[i];
			Method[] properties= getProperties(curr, fgNoFilter);
			
			int nProperties= properties.length;
			if (nProperties != 0) {
				String className= Signature.getSimpleName(curr.getName());
				buf.append("case ASTNode." + toConstantName(className) + ":");
				buf.append("return new int[] {");
				for (int k= 0; k < properties.length; k++) {
					if (k > 0) {
						buf.append(",");
					}
					buf.append(toConstantName(getPropertyName(properties[k])));
				}
				buf.append("};");
			}
		}
		buf.append("}");	
		buf.append("return new int[0];");
		buf.append("}\n\n");
	}		
	
	
	private HashMap getPropertyList(Class[] astClases) {
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
	
	private HashMap createClassList(Class[] astClases) {
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
	
	private static String toVariableName(String string) {
		return Character.toLowerCase(string.charAt(0)) + string.substring(1);
	}
	
}