/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.TextEditBasedChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.code.TargetProvider;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * The ChangeCalculator class is responsible for calculating and managing the changes to be made
 * during the refactoring process. It analyzes and modifies the target method's declaration, handles
 * instance usages, adds static modifiers, updates type parameters, deletes override annotations,
 * and modifies method invocations.
 */
public class ChangeCalculator {

	/**
	 * The {@code MethodDeclaration} object representing the selected method on which the
	 * refactoring should be performed. This field is used to analyze and modify the method's
	 * declaration during the refactoring process.
	 */
	private MethodDeclaration fTargetMethodDeclaration;

	/**
	 * The {@code IMethod} object representing the selected method on which the refactoring should
	 * be performed.
	 */
	private IMethod fTargetMethod;

	/**
	 * Manages all changes to the source code that will be performed at the end of the refactoring.
	 */
	private TextEditBasedChangeManager fChangeManager;

	/**
	 * This AST represents the abstract syntax tree of the target method declaration used in the
	 * refactoring process.
	 */
	private AST fTargetMethodDeclarationAST;

	/**
	 * This ASTRewrite object is used to modify the AST (Abstract Syntax Tree) of the target method
	 * declaration during the refactoring process.
	 */
	private ASTRewrite fTargetMethodDeclarationASTRewrite;

	/**
	 * The unique name of the parameter used to access instance variables or instance methods.
	 */
	private String fParameterName;

	private InstanceUsageRewriter fInstanceUsageRewriter;

	private FinalConditionsChecker fFinalConditionsChecker;

	/**
	 * Constructs a new ChangeCalculator with the given parameters.
	 *
	 * @param targetMethodDeclaration The method declaration of the target method.
	 * @param targetMethod The target method.
	 * @param finalConditionsChecker The FinalConditionsChecker instance used for final conditions
	 *            checking during the refactoring. It must not be null and provides necessary checks
	 *            during the refactoring process.
	 */
	public ChangeCalculator(MethodDeclaration targetMethodDeclaration, IMethod targetMethod, FinalConditionsChecker finalConditionsChecker) {
		fTargetMethodDeclaration= targetMethodDeclaration;
		fTargetMethod= targetMethod;
		fTargetMethodDeclarationAST= fTargetMethodDeclaration.getAST();
		fTargetMethodDeclarationASTRewrite= ASTRewrite.create(fTargetMethodDeclarationAST);
		fParameterName= generateUniqueParameterName();
		fChangeManager= new TextEditBasedChangeManager();
		fFinalConditionsChecker= finalConditionsChecker;
	}

	/**
	 * Retrieves an array of TextEditBasedChange objects representing the changes made by the
	 * ChangeCalculator. Changes are computed if they are not executed yet.
	 *
	 * @return An array of TextEditBasedChange objects containing the changes made by the
	 *         ChangeCalculator.
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public TextEditBasedChange[] getOrComputeChanges() throws JavaModelException {
		if (fChangeManager.getAllChanges().length != 0) {
			return fChangeManager.getAllChanges();
		}
		modifyMethodDeclaration();
		return fChangeManager.getAllChanges();
	}

	/**
	 * Computes the edit for the target method declaration and adds it to the change manager.
	 *
	 * @throws JavaModelException is thrown when the underlying compilation units buffer could not
	 *             be accessed.
	 */
	private void computeMethodDeclarationEdit() throws JavaModelException {
		//Changes can't be applied to directly to AST, edits are saved in fChangeManager
		TextEdit methodDeclarationEdit= fTargetMethodDeclarationASTRewrite.rewriteAST();
		addEditToChangeManager(methodDeclarationEdit, fTargetMethod.getCompilationUnit());
	}

	/**
	 * This method uses an InstanceUsageRewriter to rewrite the instance usages in the target method
	 * declaration. The rewritten AST is updated in the fTargetMethodDeclarationASTRewrite. The
	 * fTargetMethodhasInstanceUsage flag is also updated based on the instance usage rewriting.
	 *
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	private void rewriteInstanceUsages() throws JavaModelException {
		fInstanceUsageRewriter= new InstanceUsageRewriter(fParameterName, fTargetMethodDeclarationASTRewrite, fTargetMethodDeclarationAST, fTargetMethodDeclaration, fFinalConditionsChecker);
		fTargetMethodDeclaration.getBody().accept(fInstanceUsageRewriter);

		//check if method would unintentionally hide method of parent class
		fFinalConditionsChecker.checkMethodWouldHideParentMethod(fInstanceUsageRewriter.getTargetMethodhasInstanceUsage(), fTargetMethod);
		//While refactoring the method signature might change; ensure the revised method doesn't unintentionally override an existing one.
		fFinalConditionsChecker.checkMethodIsNotDuplicate(fTargetMethodDeclaration, fTargetMethod);
	}

	/**
	 * Modifies the method declaration by performing a series of transformations and updates. This
	 * method is used to convert an instance method to a static method. The following steps are
	 * executed in order:
	 *
	 * 1. Add the 'static' modifier to the target method.
	 *
	 * 2. Change instance usages ("this" and "super") within the method body to a specified
	 * parameter name. Also, set the 'fTargetMethodhasInstanceUsage' flag if there were instance
	 * usages found.
	 *
	 * 3. If the 'fTargetMethodhasInstanceUsage' flag is set (i.e., the method had instance usages):
	 * Add an instance parameter to the newly static method to ensure it can still access
	 * class-level state and behavior.
	 *
	 * 4. Update the type parameter list of the method declaration and insert new type parameters to
	 * the JavaDoc. The method uses 'fStatus' to merge any updates or error messages.
	 *
	 * 5. Remove any 'override' annotations, as a static method cannot have an 'override'
	 * annotation.
	 *
	 * 6. Compute the necessary edits for the modified method declaration.
	 *
	 * @throws JavaModelException if there is an issue with the Java model while modifying the
	 *             method declaration.
	 */
	public void modifyMethodDeclaration() throws JavaModelException {
		addStaticModifierToTargetMethod();
		rewriteInstanceUsages();
		if (fInstanceUsageRewriter.getTargetMethodhasInstanceUsage()) {
			addInstanceAsParameterIfUsed();
		}
		updateTargetMethodTypeParamList();
		deleteOverrideAnnotation();
		computeMethodDeclarationEdit();
	}

	/**
	 * This method uses a ModifierRewrite to add the static modifier to the target method
	 * declaration. The fTargetMethodDeclarationASTRewrite is updated with the modified AST.
	 */
	private void addStaticModifierToTargetMethod() {
		ModifierRewrite modRewrite= ModifierRewrite.create(fTargetMethodDeclarationASTRewrite, fTargetMethodDeclaration);
		modRewrite.setModifiers(fTargetMethodDeclaration.getModifiers() | Modifier.STATIC, null);
	}

	private String generateUniqueParameterName() {
		String className= ((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().toString();
		List<SingleVariableDeclaration> parameters= fTargetMethodDeclaration.parameters();

		String baseParameterName= Character.toLowerCase(className.charAt(0)) + className.substring(1);
		int duplicateCount= 1;
		String uniqueParameterName= baseParameterName;

		while (parameterNameExists(uniqueParameterName, parameters)) {
			duplicateCount++;
			uniqueParameterName= baseParameterName + duplicateCount;
		}

		return uniqueParameterName;
	}

	private boolean parameterNameExists(String parameterName, List<SingleVariableDeclaration> parameters) {
		for (SingleVariableDeclaration param : parameters) {
			if (param.getName().getIdentifier().equals(parameterName)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Adds an instance parameter to the target method declaration to ensure the access of fields or
	 * instance methods. The new parameter is inserted at the first position in the list of
	 * parameters. After adding the parameter, the JavaDocs associated with the target method
	 * declaration are updated to reflect the changes.
	 *
	 * @throws JavaModelException is thrown when the underlying compilation units buffer could not
	 *             be accessed.
	 */
	private void addInstanceAsParameterIfUsed() throws JavaModelException {
		ListRewrite lrw= fTargetMethodDeclarationASTRewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		lrw.insertFirst(generateNewParameter(), null);
		//Changes to fTargetMethodDeclaration's signature need to be adjusted in JavaDocs too
		updateJavaDocs();
	}

	private void updateJavaDocs() {
		Javadoc javadoc= fTargetMethodDeclaration.getJavadoc();
		if (javadoc != null) {
			TagElement newParameterTag= fTargetMethodDeclarationAST.newTagElement();
			newParameterTag.setTagName(TagElement.TAG_PARAM);
			newParameterTag.fragments().add(fTargetMethodDeclarationAST.newSimpleName(fParameterName));
			ListRewrite tagsRewrite= fTargetMethodDeclarationASTRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			tagsRewrite.insertFirst(newParameterTag, null);
		}
	}

	private SingleVariableDeclaration generateNewParameter() throws JavaModelException {
		String className= ((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().toString();
		IType parentType= fTargetMethod.getDeclaringType();
		ITypeParameter[] classTypeParameters= parentType.getTypeParameters();
		SingleVariableDeclaration newParam= fTargetMethodDeclarationAST.newSingleVariableDeclaration();

		//If generic TypeParameters exist in class the newParam type needs to be parameterized
		if (classTypeParameters.length != 0) {
			SimpleType simpleType= fTargetMethodDeclarationAST.newSimpleType(fTargetMethodDeclarationAST.newName(className));
			ParameterizedType parameterizedType= fTargetMethodDeclarationAST.newParameterizedType(simpleType);
			for (int parameterNumber= 0; parameterNumber < classTypeParameters.length; parameterNumber++) {
				SimpleType typeParameter= fTargetMethodDeclarationAST.newSimpleType(fTargetMethodDeclarationAST.newSimpleName(classTypeParameters[parameterNumber].getElementName()));
				parameterizedType.typeArguments().add(typeParameter);
			}
			newParam.setType(parameterizedType);
		} else {
			newParam.setType(fTargetMethodDeclarationAST.newSimpleType(fTargetMethodDeclarationAST.newName(className)));
		}
		newParam.setName(fTargetMethodDeclarationAST.newSimpleName(fParameterName));
		return newParam;
	}

	/**
	 * This method updates the type parameter list of the target method declaration based on the
	 * class type parameters. If the method requires a type parameter (either because it has method
	 * parameters of that type, or it has instance usage), and the type parameter is not already
	 * present in the method's type parameter list, it is inserted. Additionally, the JavaDocs
	 * associated with the target method declaration are updated to reflect the changes.
	 *
	 * @throws JavaModelException if the type parameters of the parentType do not exist or if an
	 *             exception occurs while accessing its corresponding resource.
	 */
	private void updateTargetMethodTypeParamList() throws JavaModelException {
		IType parentType= fTargetMethod.getDeclaringType();
		ITypeParameter[] classTypeParameters= parentType.getTypeParameters();
		ListRewrite typeParamsRewrite= fTargetMethodDeclarationASTRewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.TYPE_PARAMETERS_PROPERTY);
		Javadoc javadoc= fTargetMethodDeclaration.getJavadoc();
		List<String> methodParameterTypes= getMethodParameterTypes();
		List<String> methodTypeParametersNames= getTypeParameterNames();

		if (classTypeParameters.length != 0) {
			for (int i= 0; i < classTypeParameters.length; i++) {
				TypeParameter typeParameter= generateTypeParameter(classTypeParameters, i);

				if (fFinalConditionsChecker.getStatus().hasError()) {
					return;
				}
				//Check if method needs this TypeParameter (only if one or more methodParams are of this type OR method has instance usage OR an instance of parent class is used as methodParam)
				String typeParamName= typeParameter.getName().getIdentifier();
				String typeParamNameAsArray= typeParamName + "[]"; //$NON-NLS-1$
				boolean paramIsNeeded= methodParameterTypes.contains(typeParamName) || methodParameterTypes.contains(typeParamNameAsArray);
				if (fInstanceUsageRewriter.getTargetMethodhasInstanceUsage() || paramIsNeeded) {
					//only insert if typeParam not already existing
					if (!methodTypeParametersNames.contains(typeParameter.getName().getIdentifier())) {
						typeParamsRewrite.insertLast(typeParameter, null);
						addNewTypeParamsToJavaDoc(javadoc, typeParameter);
					}
				}
			}
		}
		return;
	}

	private TypeParameter generateTypeParameter(ITypeParameter[] classTypeParameters, int i) throws JavaModelException {
		String[] bounds= classTypeParameters[i].getBounds();
		TypeParameter typeParameter= fTargetMethodDeclarationAST.newTypeParameter();
		typeParameter.setName(fTargetMethodDeclarationAST.newSimpleName(classTypeParameters[i].getElementName()));
		for (String bound : bounds) {
			//WildCardTypes are not allowed as bounds
			fFinalConditionsChecker.checkBoundNotContainingWildCardType(bound);
			if (!fFinalConditionsChecker.getStatus().hasError()) {
				SimpleType boundType= fTargetMethodDeclarationAST.newSimpleType(fTargetMethodDeclarationAST.newSimpleName(bound));
				typeParameter.typeBounds().add(boundType);
			}
		}
		return typeParameter;
	}

	private List<String> getMethodParameterTypes() {
		List<SingleVariableDeclaration> methodParams= fTargetMethodDeclaration.parameters();
		List<String> methodParameterTypes= new ArrayList<>(methodParams.size());
		for (SingleVariableDeclaration methodParam : methodParams) {
			Type type= methodParam.getType();
			//if type is parameterized, then those typeParams need to be included in TypeParamList of method
			if (type.isParameterizedType()) {
				ParameterizedType parameterizedType= (ParameterizedType) type;
				List<Type> typeParamsOfMethodParam= parameterizedType.typeArguments();
				for (Type typeParamOfMethodParam : typeParamsOfMethodParam) {
					methodParameterTypes.add(typeParamOfMethodParam.resolveBinding().getName());
				}
			}
			String typeName= type.toString();
			methodParameterTypes.add(typeName);
		}
		return methodParameterTypes;
	}

	private List<String> getTypeParameterNames() {
		List<TypeParameter> methodTypeParameters= fTargetMethodDeclaration.typeParameters();
		List<String> methodTypeParametersNames= new ArrayList<>(methodTypeParameters.size());
		for (TypeParameter methodTypeParam : methodTypeParameters) {
			methodTypeParametersNames.add(methodTypeParam.getName().getIdentifier());
		}
		return methodTypeParametersNames;
	}

	private void addNewTypeParamsToJavaDoc(Javadoc javadoc, TypeParameter typeParameter) {
		if (javadoc != null) {
			//add new type params to javaDoc
			TextElement textElement= fTargetMethodDeclarationAST.newTextElement();
			textElement.setText("<" + typeParameter.getName().getIdentifier() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			TagElement newParameterTag= fTargetMethodDeclarationAST.newTagElement();
			newParameterTag.setTagName(TagElement.TAG_PARAM);
			newParameterTag.fragments().add(textElement);
			ListRewrite tagsRewrite= fTargetMethodDeclarationASTRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			tagsRewrite.insertLast(newParameterTag, null);
		}
	}

	/**
	 * This method removes the override annotation from the modifiers of the target method
	 * declaration. The fTargetMethodDeclarationASTRewrite is updated with the modified AST.
	 */
	private void deleteOverrideAnnotation() {
		ListRewrite listRewrite= fTargetMethodDeclarationASTRewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		for (Object obj : fTargetMethodDeclaration.modifiers()) {
			if (obj instanceof org.eclipse.jdt.core.dom.MarkerAnnotation markerAnnotation) {
				if (markerAnnotation.getTypeName().getFullyQualifiedName().equals("Override")) { //$NON-NLS-1$
					listRewrite.remove(markerAnnotation, null);
				}
			}
		}
	}

	private void addEditToChangeManager(TextEdit editToAdd, ICompilationUnit iCompilationUnit) {
		//get CompilationUnitChange from ChangeManager, otherwise create one
		CompilationUnitChange compilationUnitChange= (CompilationUnitChange) fChangeManager.get(iCompilationUnit);

		//get all Edits from compilationUnitChange, otherwise create a MultiTextEdit
		MultiTextEdit allTextEdits= (MultiTextEdit) compilationUnitChange.getEdit();
		if (allTextEdits == null) {
			allTextEdits= new MultiTextEdit();
		}
		allTextEdits.addChild(editToAdd);
		String changeName= Messages.format(RefactoringCoreMessages.MakeStaticRefactoring_change_name, iCompilationUnit.getElementName());
		CompilationUnitChange newCompilationUnitChange= new CompilationUnitChange(changeName, iCompilationUnit);
		newCompilationUnitChange.setEdit(allTextEdits);
		fChangeManager.manage(iCompilationUnit, newCompilationUnitChange);
	}

	/**
	 * This method retrieves all invocations of the refactored method in the affected compilation
	 * units. It checks if there are method references to the selected method, cancels the
	 * refactoring if found. It modifies the method invocations by applying necessary edits to the
	 * AST. The edits are added to the change manager for each affected compilation unit.
	 *
	 * @param progressMonitor The progress monitor to report the progress of the operation.
	 * @param targetMethodBinding The method binding of the target method.
	 * @throws CoreException if an error occurs during handling method invocations.
	 */
	public void handleMethodInvocations(IProgressMonitor progressMonitor, IMethodBinding targetMethodBinding) throws CoreException {
		//Provides all invocations of the refactored method in the workspace.
		TargetProvider targetProvider= TargetProvider.create(fTargetMethodDeclaration);
		targetProvider.initialize();
		IProgressMonitor sliceProgessMonitor= progressMonitor.slice(1);
		ICompilationUnit[] affectedICompilationUnits= targetProvider.getAffectedCompilationUnits(fFinalConditionsChecker.getStatus(), new ReferencesInBinaryContext(""), sliceProgessMonitor); //$NON-NLS-1$
		for (ICompilationUnit affectedICompilationUnit : affectedICompilationUnits) {

			//Check if MethodReferences use selected method -> cancel refactoring
			CompilationUnit affectedCompilationUnit= convertICUtoCU(affectedICompilationUnit);
			affectedCompilationUnit.accept(new MethodReferenceFinder(targetMethodBinding, fFinalConditionsChecker));

			if (fFinalConditionsChecker.getStatus().hasFatalError()) {
				return;
			}

			BodyDeclaration[] bodies= targetProvider.getAffectedBodyDeclarations(affectedICompilationUnit, null);
			MultiTextEdit multiTextEdit= new MultiTextEdit();
			for (BodyDeclaration body : bodies) {
				ASTNode[] invocations= targetProvider.getInvocations(body, null);
				for (ASTNode invocationASTNode : invocations) {
					MethodInvocation invocation= (MethodInvocation) invocationASTNode;
					modifyMethodInvocation(multiTextEdit, invocation);
				}
			}

			if (fFinalConditionsChecker.getStatus().hasFatalError()) {
				return;
			}

			addEditToChangeManager(multiTextEdit, affectedICompilationUnit);
		}
		return;
	}

	private CompilationUnit convertICUtoCU(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);

		return (CompilationUnit) parser.createAST(null);
	}

	private void modifyMethodInvocation(MultiTextEdit multiTextEdit, MethodInvocation invocation) throws JavaModelException {
		AST ast= invocation.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		if (fInstanceUsageRewriter.getTargetMethodhasInstanceUsage()) {
			ASTNode newArg;
			if (invocation.getExpression() != null) {
				newArg= ASTNode.copySubtree(ast, invocation.getExpression()); // copy the expression
			} else {
				// We need to find the class that this invocation is inside
				ASTNode parent= findParentClass(invocation);
				boolean isMember= isMember(parent);

				// If the current class is a member of another class, we need to qualify this
				if (isMember) {
					newArg= qualifyThisExpression(invocation, ast);
				} else {
					newArg= ast.newThisExpression();
				}
			}
			ListRewrite listRewrite= rewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
			listRewrite.insertFirst(newArg, null);
		}

		SimpleName optionalExpression= ast.newSimpleName(((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().getIdentifier());
		rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, optionalExpression, null);

		TextEdit methodInvocationEdit= rewrite.rewriteAST();
		multiTextEdit.addChild(methodInvocationEdit);
	}

	private ASTNode qualifyThisExpression(MethodInvocation invocation, AST ast) {
		ASTNode newArg;
		ThisExpression thisExpression= ast.newThisExpression();

		// Find the outer class
		IMethodBinding invocationBinding= invocation.resolveMethodBinding();
		ITypeBinding outerClassBinding= invocationBinding.getDeclaringClass();
		String outerClassName= outerClassBinding.getName();

		// Qualify this with the name of the outer class
		thisExpression.setQualifier(ast.newSimpleName(outerClassName));
		newArg= thisExpression;
		return newArg;
	}

	private boolean isMember(ASTNode parent) {
		boolean isMember= false;
		if (parent instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration currentClass= (AbstractTypeDeclaration) parent;
			if (currentClass.isMemberTypeDeclaration()) {
				isMember= true;
			}
		} else if (parent instanceof AnonymousClassDeclaration) {
			isMember= true;
		}
		return isMember;
	}

	private ASTNode findParentClass(MethodInvocation invocation) {
		ASTNode parent= invocation;
		while ((!(parent instanceof AbstractTypeDeclaration)) && (!(parent instanceof AnonymousClassDeclaration))) {
			parent= parent.getParent();
		}
		return parent;
	}
}
