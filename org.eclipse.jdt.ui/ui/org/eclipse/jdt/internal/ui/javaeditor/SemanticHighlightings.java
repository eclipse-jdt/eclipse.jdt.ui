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

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

import org.eclipse.jdt.internal.corext.dom.Bindings;

import org.eclipse.jdt.ui.PreferenceConstants;


/**
 * Semantic highlightings
 * 
 * @since 3.0
 */
public class SemanticHighlightings {

	/**
	 * A named preference part that controls the highlighting of static final fields.
	 */
	public static final String STATIC_FINAL_FIELD="staticFinalField"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of static fields.
	 */
	public static final String STATIC_FIELD="staticField"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of fields.
	 */
	public static final String FIELD="field"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of method declarations.
	 */
	public static final String METHOD_DECLARATION="methodDeclarationName"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of static method invocations.
	 */
	public static final String STATIC_METHOD_INVOCATION="staticMethodInvocation"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of inherited method invocations.
	 */
	public static final String INHERITED_METHOD_INVOCATION="inheritedMethodInvocation"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of annotation element references.
	 * @since 3.1
	 */
	public static final String ANNOTATION_ELEMENT_REFERENCE="annotationElementReference"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of abstract method invocations.
	 */
	public static final String ABSTRACT_METHOD_INVOCATION="abstractMethodInvocation"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of local variables.
	 */
	public static final String LOCAL_VARIABLE_DECLARATION="localVariableDeclaration"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of local variables.
	 */
	public static final String LOCAL_VARIABLE="localVariable"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of parameter variables.
	 */
	public static final String PARAMETER_VARIABLE="parameterVariable"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of deprecated members.
	 */
	public static final String DEPRECATED_MEMBER="deprecatedMember"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of type parameters.
	 * @since 3.1
	 */
	public static final String TYPE_PARAMETER="typeParameter"; //$NON-NLS-1$

	/**
	 * Semantic highlightings
	 */
	private static SemanticHighlighting[] fgSemanticHighlightings;

	/**
	 * Semantic highlighting for static final fields.
	 */
	private static class StaticFinalFieldHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return STATIC_FINAL_FIELD;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.staticFinalField"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			return binding != null && binding.getKind() == IBinding.VARIABLE && ((IVariableBinding)binding).isField() && (binding.getModifiers() & (Modifier.FINAL | Modifier.STATIC)) == (Modifier.FINAL | Modifier.STATIC);
		}
	}
	
	/**
	 * Semantic highlighting for static fields.
	 */
	private static class StaticFieldHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return STATIC_FIELD;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.staticField"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			return binding != null && binding.getKind() == IBinding.VARIABLE && ((IVariableBinding)binding).isField() && (binding.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
		}
	}
	
	/**
	 * Semantic highlighting for fields.
	 */
	private static class FieldHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return FIELD;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.field"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			return binding != null && binding.getKind() == IBinding.VARIABLE && ((IVariableBinding)binding).isField();
		}
	}
	
	/**
	 * Semantic highlighting for method declarations.
	 */
	private static class MethodDeclarationHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return METHOD_DECLARATION;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.methodDeclaration"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#isMatched(org.eclipse.jdt.core.dom.ASTNode)
		 */
		public boolean consumes(SemanticToken token) {
			StructuralPropertyDescriptor location= token.getNode().getLocationInParent();
			return location == MethodDeclaration.NAME_PROPERTY || location == AnnotationTypeMemberDeclaration.NAME_PROPERTY;
		}
	}
	
	/**
	 * Semantic highlighting for static method invocations.
	 */
	private static class StaticMethodInvocationHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return STATIC_METHOD_INVOCATION;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.staticMethodInvocation"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#isMatched(org.eclipse.jdt.core.dom.ASTNode)
		 */
		public boolean consumes(SemanticToken token) {
			SimpleName node= token.getNode();
			if (node.isDeclaration())
				return false;
			
			IBinding binding= token.getBinding();
			return binding != null && binding.getKind() == IBinding.METHOD && (binding.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
		}
	}
	
	/**
	 * Semantic highlighting for annotation element references.
	 * @since 3.1
	 */
	private static class AnnotationElementReferenceHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return ANNOTATION_ELEMENT_REFERENCE;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.annotationElementReference"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#isMatched(org.eclipse.jdt.core.dom.ASTNode)
		 */
		public boolean consumes(SemanticToken token) {
			SimpleName node= token.getNode();
			if (node.getParent() instanceof MemberValuePair) {
				IBinding binding= token.getBinding();
				boolean isAnnotationElement= binding != null && binding.getKind() == IBinding.METHOD;
				
				return isAnnotationElement;
			}
			
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for abstract method invocations.
	 */
	private static class AbstractMethodInvocationHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return ABSTRACT_METHOD_INVOCATION;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.abstractMethodInvocation"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#isMatched(org.eclipse.jdt.core.dom.ASTNode)
		 */
		public boolean consumes(SemanticToken token) {
			SimpleName node= token.getNode();
			if (node.isDeclaration())
				return false;
			
			IBinding binding= token.getBinding();
			boolean isAbstractMethod= binding != null && binding.getKind() == IBinding.METHOD && (binding.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT;
			if (!isAbstractMethod)
				return false;

			// filter out annotation value references
			ITypeBinding declaringType= ((IMethodBinding)binding).getDeclaringClass();
			if (declaringType.isAnnotation())
				return false;
			
			return true;
		}
	}
	
	/**
	 * Semantic highlighting for inherited method invocations.
	 */
	private static class InheritedMethodInvocationHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return INHERITED_METHOD_INVOCATION;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.inheritedMethodInvocation"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#isMatched(org.eclipse.jdt.core.dom.ASTNode)
		 */
		public boolean consumes(SemanticToken token) {
			SimpleName node= token.getNode();
			if (node.isDeclaration())
				return false;

			IBinding binding= token.getBinding();
			if (binding == null || binding.getKind() != IBinding.METHOD)
				return false;
			
			ITypeBinding currentType= Bindings.getBindingOfParentType(node);
			ITypeBinding declaringType= ((IMethodBinding)binding).getDeclaringClass();
			if (currentType == declaringType)
				return false;
			
			return Bindings.isSuperType(declaringType, currentType);
		}
	}
	
	/**
	 * Semantic highlighting for local variable declarations.
	 */
	private static class LocalVariableDeclarationHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return LOCAL_VARIABLE_DECLARATION;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.localVariableDeclaration"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		public boolean consumes(SemanticToken token) {
			SimpleName node= token.getNode();
			StructuralPropertyDescriptor location= node.getLocationInParent();
			if (location == VariableDeclarationFragment.NAME_PROPERTY || location == SingleVariableDeclaration.NAME_PROPERTY) {
				ASTNode parent= node.getParent();
				if (parent instanceof VariableDeclaration) {
					parent= parent.getParent();
					return parent == null || !(parent instanceof FieldDeclaration); 
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for local variables.
	 */
	private static class LocalVariableHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return LOCAL_VARIABLE;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.localVariable"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			if (binding != null && binding.getKind() == IBinding.VARIABLE && !((IVariableBinding) binding).isField()) {
				ASTNode decl= token.getRoot().findDeclaringNode(binding);
				return decl instanceof VariableDeclaration;
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for parameter variables.
	 */
	private static class ParameterVariableHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return PARAMETER_VARIABLE;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.parameterVariable"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			if (binding != null && binding.getKind() == IBinding.VARIABLE && !((IVariableBinding) binding).isField()) {
				ASTNode decl= token.getRoot().findDeclaringNode(binding);
				return decl != null && decl.getLocationInParent() == MethodDeclaration.PARAMETERS_PROPERTY;
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for deprecated members.
	 */
	private static class DeprecatedMemberHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return DEPRECATED_MEMBER;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(150, 150, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.deprecatedMember"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		public boolean consumes(SemanticToken token) {
			IBinding binding;
			// work around: https://bugs.eclipse.org/bugs/show_bug.cgi?id=62605
			ASTNode parent= token.getNode().getParent();
			if (parent != null && parent.getLocationInParent() ==  ClassInstanceCreation.TYPE_PROPERTY)
				binding= ((ClassInstanceCreation) parent.getParent()).resolveConstructorBinding();
			else
				binding= token.getBinding();
			return binding != null ? binding.isDeprecated() : false;
		}
	}

	/**
	 * Semantic highlighting for generic type parameters.
	 * @since 3.1
	 */
	private static class TypeParameterHighlighting extends SemanticHighlighting {
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#getPreferenceKey()
		 */
		public String getPreferenceKey() {
			return TYPE_PARAMETER;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextColor()
		 */
		public RGB getDefaultTextColor() {
			return new RGB(100, 70, 50);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDefaultTextStyleBold()
		 */
		public boolean isBoldByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isItalicByDefault()
		 */
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#isEnabledByDefault()
		 */
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#getDisplayName()
		 */
		public String getDisplayName() {
			return JavaEditorMessages.getString("SemanticHighlighting.typeParameter"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		public boolean consumes(SemanticToken token) {
			if (token.getNode().getParent() instanceof WildcardType)
				return true;
			IBinding binding= token.getBinding();
			if (binding != null) {
				ASTNode decl= token.getRoot().findDeclaringNode(binding);
				return decl instanceof TypeParameter; 
			}
			return false;
		}
	}

	/**
	 * A named preference that controls the given semantic highlighting's color.
	 * 
	 * @param semanticHighlighting the semantic highlighting
	 * @return the color preference key
	 */
	public static String getColorPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey() + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;
	}

	/**
	 * A named preference that controls if the given semantic highlighting has the text attribute bold.
	 * 
	 * @param semanticHighlighting the semantic highlighting
	 * @return the bold preference key
	 */
	public static String getBoldPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey() + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
	}
	
	/**
	 * A named preference that controls if the given semantic highlighting has the text attribute italic.
	 * 
	 * @param semanticHighlighting the semantic highlighting
	 * @return the italic preference key
	 */
	public static String getItalicPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey() + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
	}
	
	/**
	 * A named preference that controls if the given semantic highlighting is enabled.
	 * 
	 * @param semanticHighlighting the semantic highlighting
	 * @return the enabled preference key
	 */
	public static String getEnabledPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey() + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;
	}
	
	/**
	 * @return The semantic highlightings, the order defines the precedence of matches, the first match wins.
	 */
	public static SemanticHighlighting[] getSemanticHighlightings() {
		if (fgSemanticHighlightings == null)
			fgSemanticHighlightings= new SemanticHighlighting[] {
				new DeprecatedMemberHighlighting(),
				new StaticFinalFieldHighlighting(),
				new StaticFieldHighlighting(),
				new FieldHighlighting(),
				new MethodDeclarationHighlighting(),
				new StaticMethodInvocationHighlighting(),
				new AbstractMethodInvocationHighlighting(),
				new AnnotationElementReferenceHighlighting(),
				new InheritedMethodInvocationHighlighting(),
				new ParameterVariableHighlighting(),
				new LocalVariableDeclarationHighlighting(),
				new LocalVariableHighlighting(),
				new TypeParameterHighlighting(),
			};
		return fgSemanticHighlightings;
	}
	
	/**
	 * Initialize default preferences in the given preference store.
	 * @param store The preference store
	 */
	public static void initDefaults(IPreferenceStore store) {
		SemanticHighlighting[] semanticHighlightings= getSemanticHighlightings();
		for (int i= 0, n= semanticHighlightings.length; i < n; i++) {
			SemanticHighlighting semanticHighlighting= semanticHighlightings[i];
			PreferenceConverter.setDefault(store, SemanticHighlightings.getColorPreferenceKey(semanticHighlighting), semanticHighlighting.getDefaultTextColor());
			store.setDefault(SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting), semanticHighlighting.isBoldByDefault());
			store.setDefault(SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting), semanticHighlighting.isItalicByDefault());
			store.setDefault(SemanticHighlightings.getEnabledPreferenceKey(semanticHighlighting), semanticHighlighting.isEnabledByDefault());
		}
	}
	
	/**
	 * Tests whether <code>event</code> in <code>store</code> affects the
	 * enablement of semantic highlighting.
	 * 
	 * @param store the preference store where <code>event</code> was observed
	 * @param event the property change under examination
	 * @return <code>true</code> if <code>event</code> changed semantic
	 *         highlighting enablement, <code>false</code> if it did not
	 * @since 3.1
	 */
	public static boolean affectsEnablement(IPreferenceStore store, PropertyChangeEvent event) {
		String relevantKey= null;
		SemanticHighlighting[] highlightings= getSemanticHighlightings();
		for (int i= 0; i < highlightings.length; i++) {
			if (event.getProperty().equals(getEnabledPreferenceKey(highlightings[i]))) {
				relevantKey= event.getProperty();
				break;
			}
		}
		if (relevantKey == null)
			return false;

		for (int i= 0; i < highlightings.length; i++) {
			String key= getEnabledPreferenceKey(highlightings[i]);
			if (key.equals(relevantKey))
				continue;
			if (store.getBoolean(key))
				return false; // another is still enabled or was enabled before
		}
		
		// all others are disabled, so toggling relevantKey affects the enablement
		return true;
	}
	
	/**
	 * Tests whether semantic highlighting is currently enabled.
	 * 
	 * @param store the preference store to consult
	 * @return <code>true</code> if semantic highlighting is enabled,
	 *         <code>false</code> if it is not
	 * @since 3.1
	 */
	public static boolean isEnabled(IPreferenceStore store) {
		SemanticHighlighting[] highlightings= getSemanticHighlightings();
		boolean enable= false;
		for (int i= 0; i < highlightings.length; i++) {
			String enabledKey= getEnabledPreferenceKey(highlightings[i]);
			if (store.getBoolean(enabledKey)) {
				enable= true;
				break;
			}
		}
		
		return enable;
	}

	/**
	 * Do not instantiate
	 */
	private SemanticHighlightings() {
	}
}
