/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;


public final class TypeConstraints {
	private TypeConstraints(){}
	
	public static ITypeConstraint[] create(Assignment assignment){
		return new ITypeConstraint[]{SimpleTypeConstraint.createSubtypeConstraint(new ExpressionVariable(assignment.getRightHandSide()), new ExpressionVariable(assignment.getLeftHandSide()))};
	}
	
	public static ITypeConstraint[] create(SingleVariableDeclaration svd){
		if (svd.getInitializer() == null)
			return new ITypeConstraint[0];	
		ITypeConstraint[] result= new ITypeConstraint[2];
		result[0]= SimpleTypeConstraint.createSubtypeConstraint(new ExpressionVariable(svd.getInitializer()), new ExpressionVariable(svd.getName()));
		result[1]= SimpleTypeConstraint.createDefinesConstraint(new ExpressionVariable(svd.getName()), new TypeVariable(svd.getType()));
		return result;
	}

	public static ITypeConstraint[] create(VariableDeclarationFragment vdf){
		if (vdf.getInitializer() == null)
			return new ITypeConstraint[0];	
		return new ITypeConstraint[]{SimpleTypeConstraint.createSubtypeConstraint(new ExpressionVariable(vdf.getInitializer()), new ExpressionVariable(vdf.getName()))};
	}

	public static ITypeConstraint[] create(ReturnStatement returnStatement){
		if (returnStatement.getExpression() == null)
			return new ITypeConstraint[0];
		
		ConstraintVariable returnTypeVariable= new ReturnTypeVariable(returnStatement);
		ITypeConstraint c= SimpleTypeConstraint.createSubtypeConstraint(new ExpressionVariable(returnStatement.getExpression()), returnTypeVariable);
		return new ITypeConstraint[]{c};
	}
	
	public static ITypeConstraint[] create(VariableDeclarationStatement vds){
		return getConstraintsFromFragmentList(vds.fragments(), vds.getType());
	}
	
	public static ITypeConstraint[] create(VariableDeclarationExpression vde){
		return getConstraintsFromFragmentList(vde.fragments(), vde.getType());
	}

	public static ITypeConstraint[] create(FieldDeclaration fd){
		List result= new ArrayList();
		result.addAll(Arrays.asList(getConstraintsFromFragmentList(fd.fragments(), fd.getType())));
		result.addAll(getConstraintsForHiding(fd));
		result.addAll(getConstraintsForFieldDeclaringTypes(fd));
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}

	private static Collection getConstraintsForFieldDeclaringTypes(FieldDeclaration fd) {
		Collection result= new ArrayList(fd.fragments().size());
		for (Iterator iter= fd.fragments().iterator(); iter.hasNext();) {
			VariableDeclarationFragment varDecl= (VariableDeclarationFragment)iter.next();
			IVariableBinding binding= varDecl.resolveBinding();
			Assert.isTrue(binding.isField());
			result.add(SimpleTypeConstraint.createDefinesConstraint(new DeclaringTypeVariable(binding), new RawBindingVariable(binding.getDeclaringClass())));
		}
		return result;
	}

	private static Collection getConstraintsForHiding(FieldDeclaration fd) {
		Collection result= new ArrayList();
		for (Iterator iter= fd.fragments().iterator(); iter.hasNext();) {
			result.addAll(getConstraintsForHiding((VariableDeclarationFragment)iter.next()));
		}
		return result;
	}

	private static Collection getConstraintsForHiding(VariableDeclarationFragment fragment) {
		Collection result= new ArrayList();
		IVariableBinding fieldBinding= fragment.resolveBinding();
		Assert.isTrue(fieldBinding.isField());
		Set declaringTypes= getDeclaringSuperTypes(fieldBinding);
		ConstraintVariable hiddingFieldVar= new DeclaringTypeVariable(fieldBinding);
		for (Iterator iter= declaringTypes.iterator(); iter.hasNext();) {
			ITypeBinding declaringSuperType= (ITypeBinding) iter.next();
			IVariableBinding hiddenField= findField(fieldBinding, declaringSuperType);
			Assert.isTrue(hiddenField.isField());
			ConstraintVariable hiddenFieldVar= new DeclaringTypeVariable(hiddenField);
			result.add(SimpleTypeConstraint.createStrictSubtypeConstraint(hiddingFieldVar, hiddenFieldVar));
		}
		return result;
	}

	private static ITypeConstraint[] getConstraintsFromFragmentList(List fragments, Type type) {
		int size= fragments.size();
		ConstraintVariable typeVariable= new TypeVariable(type);
		List result= new ArrayList((size * (size - 1))/2);
		for (int i= 0; i < size; i++) {
			VariableDeclarationFragment fragment1= (VariableDeclarationFragment) fragments.get(i);
			SimpleName fragment1Name= fragment1.getName();
			result.add(SimpleTypeConstraint.createDefinesConstraint(new ExpressionVariable(fragment1Name), typeVariable));
			for (int j= i + 1; j < size; j++) {
				VariableDeclarationFragment fragment2= (VariableDeclarationFragment) fragments.get(j);
				result.add(SimpleTypeConstraint.createEqualsConstraint(new ExpressionVariable(fragment1Name), new ExpressionVariable(fragment2.getName())));
			}
		}
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}

	public static ITypeConstraint[] create(MethodDeclaration declaration){
		List result= new ArrayList(declaration.parameters().size());
		IMethodBinding methodBinding= declaration.resolveBinding();
		result.add(SimpleTypeConstraint.createDefinesConstraint(new DeclaringTypeVariable(methodBinding), new RawBindingVariable(methodBinding.getDeclaringClass())));
		if (! methodBinding.isConstructor() && ! methodBinding.getReturnType().isPrimitive()){
			ConstraintVariable returnTypeBindingVariable= new ReturnTypeVariable(methodBinding);
			ConstraintVariable returnTypeVariable= new TypeVariable(declaration.getReturnType());
			ITypeConstraint defines= SimpleTypeConstraint.createDefinesConstraint(returnTypeBindingVariable, returnTypeVariable);
			result.add(defines);
		}
		for (int i= 0, n= declaration.parameters().size(); i < n; i++) {
			SingleVariableDeclaration paramDecl= (SingleVariableDeclaration)declaration.parameters().get(i);
			ConstraintVariable parameterTypeVariable= new ParameterTypeVariable(methodBinding, i);
			ConstraintVariable parameterNameVariable= new ExpressionVariable(paramDecl.getName());
			ITypeConstraint constraint= SimpleTypeConstraint.createDefinesConstraint(parameterTypeVariable, parameterNameVariable);
			result.add(constraint);
		}
		if (MethodChecks.isVirtual(methodBinding))
			result.addAll(getConstraintsForOverriding(methodBinding));
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}
	
	private static Collection getConstraintsForOverriding(IMethodBinding overriddingMethod) {
		Collection result= new ArrayList();
		Set declaringSupertypes= getDeclaringSuperTypes(overriddingMethod);
		for (Iterator iter= declaringSupertypes.iterator(); iter.hasNext();) {
			ITypeBinding superType= (ITypeBinding) iter.next();
			IMethodBinding overriddenMethod= findMethod(overriddingMethod, superType);
			Assert.isNotNull(overriddenMethod);//because we asked for declaring types
			ITypeConstraint returnTypeConstraint= SimpleTypeConstraint.createEqualsConstraint(new ReturnTypeVariable(overriddenMethod), new ReturnTypeVariable(overriddingMethod));
			result.add(returnTypeConstraint);
			Assert.isTrue(overriddenMethod.getParameterTypes().length == overriddingMethod.getParameterTypes().length);
			for (int i= 0, n= overriddenMethod.getParameterTypes().length; i < n; i++) {
				ITypeConstraint parameterTypeConstraint= SimpleTypeConstraint.createEqualsConstraint(new ParameterTypeVariable(overriddenMethod, i), new ParameterTypeVariable(overriddingMethod, i));
				result.add(parameterTypeConstraint);
			}
			ITypeConstraint declaringTypeConstraint= SimpleTypeConstraint.createStrictSubtypeConstraint(new DeclaringTypeVariable(overriddingMethod), new DeclaringTypeVariable(overriddenMethod));
			result.add(declaringTypeConstraint);
		}
		return result;
	}

	public static ITypeConstraint[] create(MethodInvocation invocation){
		List arguments= invocation.arguments();
		List result= new ArrayList(arguments.size());
		IMethodBinding methodBinding= invocation.resolveMethodBinding();
		ITypeConstraint returnTypeConstraint= getReturnTypeConstraint(invocation, methodBinding);
		if (returnTypeConstraint != null)
			result.add(returnTypeConstraint);
		result.addAll(Arrays.asList(getArgumentConstraints(arguments, methodBinding)));
		if (invocation.getExpression() != null){
			if(MethodChecks.isVirtual(methodBinding)){
				IMethodBinding[] rootDefs= getRootDefs(methodBinding);
				ConstraintVariable expressionVar= new ExpressionVariable(invocation.getExpression());
				if (rootDefs.length == 1){
					result.add(SimpleTypeConstraint.createSubtypeConstraint(expressionVar, new DeclaringTypeVariable(rootDefs[0])));
				}else{	
					ITypeConstraint[] constraints= new ITypeConstraint[rootDefs.length];
					for (int i= 0; i < rootDefs.length; i++) {
						ConstraintVariable rootDefTypeVar= new DeclaringTypeVariable(rootDefs[i]);
						constraints[i]= SimpleTypeConstraint.createSubtypeConstraint(expressionVar, rootDefTypeVar);
					}
					result.add(new CompositeOrTypeConstraint(constraints));
				}
			} else {
				ConstraintVariable typeVar= new DeclaringTypeVariable(methodBinding);
				ConstraintVariable expressionVar= new ExpressionVariable(invocation.getExpression());
				result.add(SimpleTypeConstraint.createSubtypeConstraint(expressionVar, typeVar));
			}
		}
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}

	public static ITypeConstraint[] create(SuperMethodInvocation invocation){
		List arguments= invocation.arguments();
		List result= new ArrayList(arguments.size());
		IMethodBinding methodBinding= invocation.resolveMethodBinding();
		ITypeConstraint returnTypeConstraint= getReturnTypeConstraint(invocation, methodBinding);
		if (returnTypeConstraint != null)
			result.add(returnTypeConstraint);
		result.addAll(Arrays.asList(getArgumentConstraints(arguments, methodBinding)));
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}
	
	public static ITypeConstraint[] create(ThisExpression expression){
		ConstraintVariable thisExpression= new ExpressionVariable(expression);
		ConstraintVariable declaringType= new RawBindingVariable(expression.resolveTypeBinding());//TODO fix this - can't use Decl(M) because 'this' can live outside of methods
		return new ITypeConstraint[]{SimpleTypeConstraint.createDefinesConstraint(thisExpression, declaringType)};
	}
	
	private static ITypeConstraint getReturnTypeConstraint(Expression invocation, IMethodBinding methodBinding){
		if (methodBinding.isConstructor() || methodBinding.getReturnType().isPrimitive())
			return null;
		ConstraintVariable returnTypeVariable= new ReturnTypeVariable(methodBinding);
		ConstraintVariable invocationVariable= new ExpressionVariable(invocation);
		return SimpleTypeConstraint.createDefinesConstraint(invocationVariable, returnTypeVariable);
	}
	
	public static ITypeConstraint[] create(ClassInstanceCreation instanceCreation){
		List arguments= instanceCreation.arguments();
		List result= new ArrayList(arguments.size());
		IMethodBinding methodBinding= instanceCreation.resolveConstructorBinding();
		result.addAll(Arrays.asList(getArgumentConstraints(arguments, methodBinding)));
		if (instanceCreation.getAnonymousClassDeclaration() == null){
			ConstraintVariable constructorVar= new ExpressionVariable(instanceCreation);
			ConstraintVariable typeVar= new RawBindingVariable(instanceCreation.resolveTypeBinding());
			result.add(SimpleTypeConstraint.createDefinesConstraint(constructorVar, typeVar));
		}
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);		
	}

	public static ITypeConstraint[] create(ConstructorInvocation invocation){
		List arguments= invocation.arguments();
		List result= new ArrayList(arguments.size());
		IMethodBinding methodBinding= invocation.resolveConstructorBinding();
		result.addAll(Arrays.asList(getArgumentConstraints(arguments, methodBinding)));
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}

	public static ITypeConstraint[] create(SuperConstructorInvocation invocation){
		List arguments= invocation.arguments();
		List result= new ArrayList(arguments.size());
		IMethodBinding methodBinding= invocation.resolveConstructorBinding();
		result.addAll(Arrays.asList(getArgumentConstraints(arguments, methodBinding)));
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}
	
	private static ITypeConstraint[] getArgumentConstraints(List arguments, IMethodBinding methodBinding){
		List result= new ArrayList(arguments.size());
		for (int i= 0, n= arguments.size(); i < n; i++) {
			Expression argument= (Expression) arguments.get(i);
			ConstraintVariable expressionVariable= new ExpressionVariable(argument);
			ConstraintVariable parameterTypeVariable= new ParameterTypeVariable(methodBinding, i);
			ITypeConstraint argConstraint= SimpleTypeConstraint.createSubtypeConstraint(expressionVariable, parameterTypeVariable);
			result.add(argConstraint);
		}
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);		
	}
			
	public static ITypeConstraint[] create(ArrayInitializer arrayInitializer){
		ITypeBinding arrayBinding= arrayInitializer.resolveTypeBinding();
		Assert.isTrue(arrayBinding.isArray());
		List expressions= arrayInitializer.expressions();
		ITypeConstraint[] constraints= new ITypeConstraint[expressions.size()];
		Type type= getTypeParent(arrayInitializer);
		ConstraintVariable typeVariable= new TypeVariable(type);
		for (int i= 0; i < constraints.length; i++) {
			Expression each= (Expression) expressions.get(i);
			constraints[i]= SimpleTypeConstraint.createSubtypeConstraint(new ExpressionVariable(each), typeVariable);
		}
		return constraints;
	}

	private static Type getTypeParent(ArrayInitializer arrayInitializer) {
		if (arrayInitializer.getParent() instanceof ArrayCreation){
			return ((ArrayCreation)arrayInitializer.getParent()).getType().getElementType();
		} else if (arrayInitializer.getParent() instanceof VariableDeclaration){
			VariableDeclaration parent= (VariableDeclaration)arrayInitializer.getParent();

			if (parent.getParent() instanceof VariableDeclarationStatement){
				Type type= ((VariableDeclarationStatement)parent.getParent()).getType();
				if (type.isArrayType())
					return ((ArrayType)type).getElementType();
			}

			else if (parent.getParent() instanceof VariableDeclarationExpression){
				Type type= ((VariableDeclarationExpression)parent.getParent()).getType();
				if (type.isArrayType())
					return ((ArrayType)type).getElementType();
			}

			else if (parent.getParent() instanceof FieldDeclaration){
				Type type= ((FieldDeclaration)parent.getParent()).getType();
				if (type.isArrayType())
					return ((ArrayType)type).getElementType();
			}
		}
		Assert.isTrue(false);//array initializers are allowed in only 2 places
		return null;
	}
	
	public static ITypeConstraint[] create(CastExpression castExpression){
		Expression expression= castExpression.getExpression();
		Type type= castExpression.getType();
  		ITypeConstraint definesConstraint= SimpleTypeConstraint.createDefinesConstraint(new ExpressionVariable(castExpression), new TypeVariable(castExpression.getType()));
  		if (TypeBindings.isClassBinding(expression.resolveTypeBinding()) && TypeBindings.isClassBinding(type.resolveBinding())){
			ConstraintVariable expressionVariable= new ExpressionVariable(expression);
			ConstraintVariable castExpressionVariable= new ExpressionVariable(castExpression);
			return new ITypeConstraint[]{definesConstraint, createOrOrSubtypeConstraint(expressionVariable, castExpressionVariable)};
		} else 
			return new ITypeConstraint[]{definesConstraint};
	}

	public static ITypeConstraint[] create(InstanceofExpression instanceofExpression){
		Expression expression= instanceofExpression.getLeftOperand();
		Type type= instanceofExpression.getRightOperand();
		if (TypeBindings.isClassBinding(expression.resolveTypeBinding()) && TypeBindings.isClassBinding(type.resolveBinding())){
			ConstraintVariable expressionVar= new ExpressionVariable(expression);
			ConstraintVariable typeVariable= new TypeVariable(type);
			return new ITypeConstraint[]{createOrOrSubtypeConstraint(expressionVar, typeVariable)};
		} else
			return new ITypeConstraint[0];
	}
	
	private static ITypeConstraint createOrOrSubtypeConstraint(ConstraintVariable var1, ConstraintVariable var2){
		ITypeConstraint c1= SimpleTypeConstraint.createSubtypeConstraint(var1, var2);
		ITypeConstraint c2= SimpleTypeConstraint.createSubtypeConstraint(var2, var1);
		return new CompositeOrTypeConstraint(new ITypeConstraint[]{c1, c2});
	}
	
	public static ITypeConstraint[] create(FieldAccess access){
		Expression expression= access.getExpression();
		SimpleName name= access.getName();
		IBinding binding= name.resolveBinding();
		if (! (binding instanceof IVariableBinding))
			return new ITypeConstraint[0];	
		IVariableBinding vb= (IVariableBinding)binding;
		return createConstraintsForAccessToField(vb, expression, access);
	}

	public static ITypeConstraint[] create(SuperFieldAccess access){
		SimpleName name= access.getName();
		IBinding binding= name.resolveBinding();
		if (! (binding instanceof IVariableBinding))
			return new ITypeConstraint[0];	
		IVariableBinding vb= (IVariableBinding)binding;
		return createConstraintsForAccessToField(vb, null, access);
	}
		
	public static ITypeConstraint[] create(QualifiedName qualifiedName){
		SimpleName name= qualifiedName.getName();
		Name qualifier= qualifiedName.getQualifier();
		IBinding nameBinding= name.resolveBinding();
		if (nameBinding instanceof IVariableBinding){
			IVariableBinding vb= (IVariableBinding)nameBinding;
			if (vb.isField())
				return createConstraintsForAccessToField(vb, qualifier, qualifiedName);
		} //TODO other bindings 
		return new ITypeConstraint[0];			
	}

	private static ITypeConstraint[] createConstraintsForAccessToField(IVariableBinding fieldBinding, Expression qualifier, Expression accessExpression){
		Assert.isTrue(fieldBinding.isField());
		ITypeConstraint defines= SimpleTypeConstraint.createDefinesConstraint(new ExpressionVariable(accessExpression), new RawBindingVariable(fieldBinding.getType()));
		if (qualifier == null)
			return new ITypeConstraint[]{defines};
		ConstraintVariable declaringTypeVar= new DeclaringTypeVariable(fieldBinding);
		ITypeConstraint subType= SimpleTypeConstraint.createSubtypeConstraint(new ExpressionVariable(qualifier), declaringTypeVar);
		return new ITypeConstraint[]{defines, subType};		
	}
	
	private static IVariableBinding findField(IVariableBinding fieldBinding, ITypeBinding type) {
		if (fieldBinding.getDeclaringClass().equals(type))
			return fieldBinding;
		return Bindings.findFieldInType(type, fieldBinding.getName());
	}

	/* 
	 * return Set of ITypeBindings
	 */
	private static Set getDeclaringSuperTypes(IVariableBinding fieldBinding) {
		Set allSuperTypes= TypeBindings.getSuperTypes(fieldBinding.getDeclaringClass());
		Set result= new HashSet();
		for (Iterator iter= allSuperTypes.iterator(); iter.hasNext();) {
			ITypeBinding type= (ITypeBinding) iter.next();
			if (findField(fieldBinding, type) != null)
				result.add(type);
		}
		return result;
	}
	
	//--- RootDef ----//
	private static IMethodBinding[] getRootDefs(IMethodBinding methodBinding) {
		Set declaringSuperTypes= getDeclaringSuperTypes(methodBinding); //set of ITypeBindings
		declaringSuperTypes.add(methodBinding.getDeclaringClass());
		Set result= new HashSet();
		for (Iterator iter= declaringSuperTypes.iterator(); iter.hasNext();) {
			ITypeBinding type= (ITypeBinding) iter.next();
			if (! containsASuperType(type, declaringSuperTypes))
				result.add(findMethod(methodBinding, type));
		}
		return (IMethodBinding[]) result.toArray(new IMethodBinding[result.size()]);
	}

	/* 
	 * @param declaringSuperTypes Set of ITypeBindings
	 */
	private static boolean containsASuperType(ITypeBinding type, Set declaringSuperTypes) {
		for (Iterator iter= declaringSuperTypes.iterator(); iter.hasNext();) {
			ITypeBinding maybeSuperType= (ITypeBinding) iter.next();
			if (TypeBindings.isSubtypeBindingOf(type, maybeSuperType))
				return true;
		}
		return false;
	}
	
	/* 
	 * return Set of ITypeBindings
	 */
	private static Set getDeclaringSuperTypes(IMethodBinding methodBinding) {
		Set allSuperTypes= TypeBindings.getSuperTypes(methodBinding.getDeclaringClass());
		Set result= new HashSet();
		for (Iterator iter= allSuperTypes.iterator(); iter.hasNext();) {
			ITypeBinding type= (ITypeBinding) iter.next();
			if (findMethod(methodBinding, type) != null)
				result.add(type);
		}
		return result;
	}

	private static IMethodBinding findMethod(IMethodBinding methodBinding, ITypeBinding type) {
		if (methodBinding.getDeclaringClass().equals(type))
			return methodBinding;
		return Bindings.findMethodInType(type, methodBinding.getName(), methodBinding.getParameterTypes());
	}
}