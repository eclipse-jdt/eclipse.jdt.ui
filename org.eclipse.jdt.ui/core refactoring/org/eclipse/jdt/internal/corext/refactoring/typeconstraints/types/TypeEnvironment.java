/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;


public class TypeEnvironment {
	
	/** Type code for the primitive type "int". */
	public final PrimitiveType INT= new PrimitiveType(this, PrimitiveType.INT);
	/** Type code for the primitive type "char". */
	public final PrimitiveType CHAR = new PrimitiveType(this, PrimitiveType.CHAR);
	/** Type code for the primitive type "boolean". */
	public final PrimitiveType BOOLEAN = new PrimitiveType(this, PrimitiveType.BOOLEAN);
	/** Type code for the primitive type "short". */
	public final PrimitiveType SHORT = new PrimitiveType(this, PrimitiveType.SHORT);
	/** Type code for the primitive type "long". */
	public final PrimitiveType LONG = new PrimitiveType(this, PrimitiveType.LONG);
	/** Type code for the primitive type "float". */
	public final PrimitiveType FLOAT = new PrimitiveType(this, PrimitiveType.FLOAT);
	/** Type code for the primitive type "double". */
	public final PrimitiveType DOUBLE = new PrimitiveType(this, PrimitiveType.DOUBLE);
	/** Type code for the primitive type "byte". */
	public final PrimitiveType BYTE = new PrimitiveType(this, PrimitiveType.BYTE);
	
	/** Type code for the primitive type "null". */
	public final NullType NULL = new NullType(this);
	
	final PrimitiveType[] PRIMITIVE_TYPES= {INT, CHAR, BOOLEAN, SHORT, LONG, FLOAT, DOUBLE, BYTE};
	
	private static final String[] BOXED_PRIMITIVE_NAMES= new String[] {
		"java.lang.Integer",  //$NON-NLS-1$
		"java.lang.Character",  //$NON-NLS-1$
		"java.lang.Boolean",  //$NON-NLS-1$
		"java.lang.Short",  //$NON-NLS-1$
		"java.lang.Long",  //$NON-NLS-1$
		"java.lang.Float",  //$NON-NLS-1$
		"java.lang.Double",  //$NON-NLS-1$
		"java.lang.Byte"};  //$NON-NLS-1$
	

	private Map[] fArrayTypes= new Map[] { new HashMap() };
	private Map fStandardTypes= new HashMap();
	private Map fGenericTypes= new HashMap();
	private Map fParameterizedTypes= new HashMap();
	private Map fRawTypes= new HashMap();
	private Map fTypeVariables= new HashMap();
	private Map fExtendsWildcardTypes= new HashMap();
	private Map fSuperWildcardTypes= new HashMap();
	private UnboundWildcardType fUnboundWildcardType= null;
	
	private static final int MAX_ENTRIES= 1024;
	private Map fSubTypeCache= new LinkedHashMap(50, 0.75f, true) {
		private static final long serialVersionUID= 1L;
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > MAX_ENTRIES;
		}
	};
	
	private boolean fIdentityTest;
	
	public TypeEnvironment() {
	}
	
	public boolean isIdentityTest() {
		return fIdentityTest;
	}
	
	public Map getSubTypeCache() {
		return fSubTypeCache;
	}
	
	public Type create(ITypeBinding binding) {
		if (binding.isPrimitive()) {
			return createPrimitiveType(binding);
		} else if (binding.isArray()) {
			return createArrayType(binding);
		} else if (binding.isRawType()) {
			return createRawType(binding);
		} else if (binding.isGenericType()) {
			return createGenericType(binding);
		} else if (binding.isParameterizedType()) {
			return createParameterizedType(binding);
		} else if (binding.isTypeVariable()) {
			return createTypeVariable(binding);
		} else if (binding.isWildcardType()) {
			if (binding.getBound() == null) {
				return createUnboundWildcardType(binding);
			} else if (binding.isUpperbound()) {
				return createExtendsWildCardType(binding);
			} else {
				return createSuperWildCardType(binding);
			}
		}
		if ("null".equals(binding.getName())) //$NON-NLS-1$
			return NULL;
		return createStandardType(binding);
	}
	
	PrimitiveType createUnBoxed(StandardType type) {
		String name= type.getPrettySignature();
		for (int i= 0; i < BOXED_PRIMITIVE_NAMES.length; i++) {
			if (BOXED_PRIMITIVE_NAMES[i].equals(name))
				return PRIMITIVE_TYPES[i];
		}
		return null;
	}
	
	StandardType createBoxed(PrimitiveType type, IJavaProject focus) {
		String fullyQualifiedName= BOXED_PRIMITIVE_NAMES[type.getId()];
		try {
			IType javaElementType= focus.findType(fullyQualifiedName);
			StandardType result= (StandardType)fStandardTypes.get(javaElementType);
			if (result != null)
				return result;
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setProject(focus);
			IBinding[] bindings= parser.createBindings(new IJavaElement[] {javaElementType} , null);
			return createStandardType((ITypeBinding)bindings[0]);
		} catch (JavaModelException e) {
			// fall through
		}
		return null;
	}
	
	private Type createPrimitiveType(ITypeBinding binding) {
		String name= binding.getName();
		String[] names= PrimitiveType.NAMES;
		for (int i= 0; i < names.length; i++) {
			if (name.equals(names[i])) {
				return PRIMITIVE_TYPES[i];
			}
		}
		Assert.isTrue(false, "Primitive type " + name + "unkown");  //$NON-NLS-1$//$NON-NLS-2$
		return null;
	}

	public Type getJavaLangObject() {
		return null;
	}
	
	private ArrayType createArrayType(ITypeBinding binding) {
		int index= binding.getDimensions() - 1;
		if (index >= fArrayTypes.length) {
			Map[] newArray= new Map[index + 1];
			System.arraycopy(fArrayTypes, 0, newArray, 0, fArrayTypes.length);
			fArrayTypes= newArray;
			fArrayTypes[index]= new HashMap();
		}
		Map arrayTypes= fArrayTypes[index];
		Type elementType= create(binding.getElementType());
		ArrayType result= (ArrayType)arrayTypes.get(elementType);
		if (result != null)
			return result;
		result= new ArrayType(this);
		arrayTypes.put(elementType, result);
		result.initialize(binding, elementType);
		return result;
	}
	
	private StandardType createStandardType(ITypeBinding binding) {
		IJavaElement javaElement= binding.getJavaElement();
		StandardType result= (StandardType)fStandardTypes.get(javaElement);
		if (result != null)
			return result;
		result= new StandardType(this);
		fStandardTypes.put(javaElement, result);
		result.initialize(binding, (IType)javaElement);
		return result;
	}
	
	private GenericType createGenericType(ITypeBinding binding) {
		IJavaElement javaElement= binding.getJavaElement();
		GenericType result= (GenericType)fGenericTypes.get(javaElement);
		if (result != null)
			return result;
		result= new GenericType(this);
		fGenericTypes.put(javaElement, result);
		result.initialize(binding, (IType)javaElement);
		return result;
	}
	
	private ParameterizedType createParameterizedType(ITypeBinding binding) {
		ParameterizedType key= new ParameterizedType(this);
		key.initialize(binding, (IType)binding.getJavaElement());
		fIdentityTest= false;
		ParameterizedType result= null;
		try {
			result= (ParameterizedType)fParameterizedTypes.get(key);
		} finally {
			fIdentityTest= true;
		}
		if (result != null)
			return result;
		result= key;
		fParameterizedTypes.put(key, result);
		return result;
	}
	
	private RawType createRawType(ITypeBinding binding) {
		IJavaElement javaElement= binding.getJavaElement();
		RawType result= (RawType)fRawTypes.get(javaElement);
		if (result != null)
			return result;
		result= new RawType(this);
		fRawTypes.put(javaElement, result);
		result.initialize(binding, (IType)javaElement);
		return result;
	}
	
	private Type createUnboundWildcardType(ITypeBinding binding) {
		if (fUnboundWildcardType == null) {
			fUnboundWildcardType= new UnboundWildcardType(this);
			fUnboundWildcardType.initialize(binding);
		}
		return fUnboundWildcardType;
	}
	
	private Type createExtendsWildCardType(ITypeBinding binding) {
		Type bound= create(binding.getBound());
		ExtendsWildcardType result= (ExtendsWildcardType)fExtendsWildcardTypes.get(bound);
		if (result != null)
			return result;
		result= new ExtendsWildcardType(this);
		fExtendsWildcardTypes.put(bound, result);
		result.initialize(binding);
		return result;
	}	
	
	private Type createSuperWildCardType(ITypeBinding binding) {
		Type bound= create(binding.getBound());
		SuperWildcardType result= (SuperWildcardType)fSuperWildcardTypes.get(bound);
		if (result != null)
			return result;
		result= new SuperWildcardType(this);
		fSuperWildcardTypes.put(bound, result);
		result.initialize(binding);
		return result;
	}	
	
	private TypeVariable createTypeVariable(ITypeBinding binding) {
		IJavaElement javaElement= binding.getJavaElement();
		TypeVariable result= (TypeVariable)fTypeVariables.get(javaElement);
		if (result != null)
			return result;
		result= new TypeVariable(this);
		fTypeVariables.put(javaElement, result);
		result.initialize(binding, (ITypeParameter)javaElement);
		return result;
	}
}
