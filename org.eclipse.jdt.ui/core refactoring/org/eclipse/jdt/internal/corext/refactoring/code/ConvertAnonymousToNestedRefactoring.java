/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     N.Metchev@teamphone.com - contributed fixes for
 *     - convert anonymous to nested should sometimes declare class as static [refactoring] 
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=43360)
 *     - Convert anonymous to nested: should show error if field form outer anonymous type is references [refactoring]
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=48282)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitDescriptorChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class ConvertAnonymousToNestedRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FINAL= "final"; //$NON-NLS-1$
	private static final String ATTRIBUTE_STATIC= "static"; //$NON-NLS-1$

	public static class TypeVariableFinder extends ASTVisitor {

		private final Map fBindings= new HashMap();
		private final List fFound= new ArrayList();
		
		public final boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			final ITypeBinding binding= node.resolveTypeBinding();
			if (binding != null && binding.isTypeVariable() && !fBindings.containsKey(binding.getKey())) {
				fBindings.put(binding.getKey(), binding);
				fFound.add(binding);
			}
			return true;
		}

		public final ITypeBinding[] getResult() {
			final ITypeBinding[] result= new ITypeBinding[fFound.size()];
			fFound.toArray(result);
			return result;
		}
	}

    private int fSelectionStart;
    private int fSelectionLength;
    private ICompilationUnit fCu;

    private int fVisibility; /* see Modifier */
    private boolean fDeclareFinal= true;
    private boolean fDeclareStatic;
    private String fClassName= ""; //$NON-NLS-1$
    private CodeGenerationSettings fSettings;

    private CompilationUnit fCompilationUnitNode;
    private AnonymousClassDeclaration fAnonymousInnerClassNode;
    private Set fClassNamesUsed;
	private boolean fSelfInitializing= false;

	/**
	 * Creates a new convert anonymous to nested refactoring
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param settings the code generation settings
	 * @param selectionStart
	 * @param selectionLength
	 */
    public ConvertAnonymousToNestedRefactoring(ICompilationUnit unit, CodeGenerationSettings settings, int selectionStart, int selectionLength) {
        Assert.isTrue(selectionStart >= 0);
        Assert.isTrue(selectionLength >= 0);
        Assert.isTrue(unit == null || unit.exists());
        fSelectionStart= selectionStart;
        fSelectionLength= selectionLength;
        fCu= unit;
        if (unit != null)
        	fSettings= settings;
    }

    public int[] getAvailableVisibilities() {
        if (isLocalInnerType()) {
            return new int[] { Modifier.NONE };
        } else {
            return new int[] { Modifier.PUBLIC, Modifier.PROTECTED, Modifier.NONE, Modifier.PRIVATE };
        }
    }

    public boolean isLocalInnerType() {
        return ASTNodes.getParent(ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class), ASTNode.ANONYMOUS_CLASS_DECLARATION) != null;
    }

    public int getVisibility() {
        return fVisibility;
    }

    public void setVisibility(int visibility) {
        Assert.isTrue(visibility == Modifier.PRIVATE || visibility == Modifier.NONE || visibility == Modifier.PROTECTED || visibility == Modifier.PUBLIC);
        fVisibility= visibility;
    }

    public void setClassName(String className) {
        Assert.isNotNull(className);
        fClassName= className;
    }

    public boolean canEnableSettingFinal() {
        return true;
    }

    public boolean getDeclareFinal() {
        return fDeclareFinal;
    }
    
    public boolean getDeclareStatic() {
        return fDeclareStatic;
    }
    
    public void setDeclareFinal(boolean declareFinal) {
        fDeclareFinal= declareFinal;
    }

    public void setDeclareStatic(boolean declareStatic) {
        fDeclareStatic= declareStatic;
    }

    public String getName() {
        return RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_name; 
    }

    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        RefactoringStatus result= Checks.validateModifiesFiles(
        	ResourceUtil.getFiles(new ICompilationUnit[]{fCu}),
			getValidationContext());
		if (result.hasFatalError())
		    return result;

		initAST(pm);

		if (fAnonymousInnerClassNode == null)
		    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_place_caret); 
		if (!fSelfInitializing)
			initializeDefaults();
		if (getSuperConstructorBinding() == null)
		    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_compile_errors); 
		if (getSuperTypeBinding().isLocal())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_extends_local_class); 
		return new RefactoringStatus();
    }

    private void initializeDefaults() {
        fVisibility= isLocalInnerType() ? Modifier.NONE : Modifier.PRIVATE;
        fDeclareStatic = mustInnerClassBeStatic();
    }

    private void initAST(IProgressMonitor pm) {
		fCompilationUnitNode= RefactoringASTParser.parseWithASTProvider(fCu, true, pm);
		fAnonymousInnerClassNode= getAnonymousInnerClass(NodeFinder.perform(fCompilationUnitNode, fSelectionStart, fSelectionLength));
		if (fAnonymousInnerClassNode != null) {
			final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
			if (declaration instanceof TypeDeclaration) {
				final AbstractTypeDeclaration[] nested= ((TypeDeclaration) declaration).getTypes();
				fClassNamesUsed= new HashSet(nested.length);
				for (int index= 0; index < nested.length; index++)
					fClassNamesUsed.add(nested[index].getName().getIdentifier());
			} else
				fClassNamesUsed= Collections.EMPTY_SET;
		}
	}

    private static AnonymousClassDeclaration getAnonymousInnerClass(ASTNode node) {
        if (node == null)
            return null;
        if (node instanceof AnonymousClassDeclaration)
            return (AnonymousClassDeclaration)node;
        if (node instanceof ClassInstanceCreation) {
            AnonymousClassDeclaration anon= ((ClassInstanceCreation)node).getAnonymousClassDeclaration();
            if (anon != null)
                return anon;
        }
        node= ASTNodes.getNormalizedNode(node);
        if (node.getLocationInParent() == ClassInstanceCreation.TYPE_PROPERTY) {
            AnonymousClassDeclaration anon= ((ClassInstanceCreation)node.getParent()).getAnonymousClassDeclaration();
            if (anon != null)
                return anon;
        }
        return (AnonymousClassDeclaration)ASTNodes.getParent(node, AnonymousClassDeclaration.class);
    }

    public RefactoringStatus validateInput() {
        RefactoringStatus result= Checks.checkTypeName(fClassName);
        if (result.hasFatalError())
            return result;

        if (fClassNamesUsed.contains(fClassName))
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_type_exists); 
        IMethodBinding superConstructorBinding = getSuperConstructorBinding();
        if (superConstructorBinding == null)
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_compile_errors); 
        if (fClassName.equals(superConstructorBinding.getDeclaringClass().getName()))
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_another_name); 
        if (classNameHidesEnclosingType())
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_name_hides); 
        return result;
    }

    private boolean accessesAnonymousFields() {
        List anonymousInnerFieldTypes = getAllEnclosingAnonymousTypesField();
        List accessedField = getAllAccessedFields();
        final Iterator it = anonymousInnerFieldTypes.iterator();
        while(it.hasNext()) {
            final IVariableBinding variableBinding = (IVariableBinding) it.next();
            final Iterator it2 = accessedField.iterator();
            while (it2.hasNext()) {
                IVariableBinding variableBinding2 = (IVariableBinding) it2.next();
                if(Bindings.equals(variableBinding, variableBinding2)) {
                    return true;
                }   
            }
        }
        return false;
    }

	private List getAllAccessedFields() {
		final List accessedFields= new ArrayList();

		ASTVisitor visitor= new ASTVisitor() {

			public boolean visit(FieldAccess node) {
				final IVariableBinding binding= node.resolveFieldBinding();
				if (binding != null && !binding.isEnumConstant())
					accessedFields.add(binding);
				return super.visit(node);
			}

			public boolean visit(QualifiedName node) {
				final IBinding binding= node.resolveBinding();
				if (binding != null && binding instanceof IVariableBinding) {
					IVariableBinding variable= (IVariableBinding) binding;
					if (!variable.isEnumConstant() && variable.isField())
						accessedFields.add(binding);
				}
				return super.visit(node);
			}

			public boolean visit(SimpleName node) {
				final IBinding binding= node.resolveBinding();
				if (binding != null && binding instanceof IVariableBinding) {
					IVariableBinding variable= (IVariableBinding) binding;
					if (!variable.isEnumConstant() && variable.isField())
						accessedFields.add(binding);
				}
				return super.visit(node);
			}

			public boolean visit(SuperFieldAccess node) {
				final IVariableBinding binding= node.resolveFieldBinding();
				if (binding != null && !binding.isEnumConstant())
					accessedFields.add(binding);
				return super.visit(node);
			}
		};
		fAnonymousInnerClassNode.accept(visitor);

		return accessedFields;
	}

    private List getAllEnclosingAnonymousTypesField() {
		final List ans= new ArrayList();
		final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
		AnonymousClassDeclaration anonymous= (AnonymousClassDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, ASTNode.ANONYMOUS_CLASS_DECLARATION);
		while (anonymous != null) {
			if (ASTNodes.isParent(anonymous, declaration)) {
				ITypeBinding binding= anonymous.resolveBinding();
				if (binding != null) {
					ans.addAll(Arrays.asList(binding.getDeclaredFields()));
				}
			} else {
				break;
			}
			anonymous= (AnonymousClassDeclaration) ASTNodes.getParent(anonymous, ASTNode.ANONYMOUS_CLASS_DECLARATION);
		}
		return ans;
	}

    private boolean classNameHidesEnclosingType() {
        ITypeBinding type= ((AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class)).resolveBinding();
        while (type != null) {
            if (fClassName.equals(type.getName()))
                return true;
            type= type.getDeclaringClass();
        }
        return false;
    }

    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
     */
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
        try {
            RefactoringStatus status= validateInput();
            if (accessesAnonymousFields())
                status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_anonymous_field_access)); 
            return status;
        } finally {
            pm.done();
        }
    }

    /*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fCu, fCompilationUnitNode);
			final ITypeBinding[] parameters= getTypeParameters();
			addNestedClass(rewrite, parameters);
			modifyConstructorCall(rewrite, parameters);
			return createChange(rewrite);
		} finally {
			pm.done();
		}
	}

	private ITypeBinding[] getTypeParameters() {
		final List parameters= new ArrayList(4);
		final ClassInstanceCreation creation= (ClassInstanceCreation) fAnonymousInnerClassNode.getParent();
		if (fDeclareStatic) {
			final TypeVariableFinder finder= new TypeVariableFinder();
			creation.accept(finder);
			return finder.getResult();
		} else {
			final MethodDeclaration declaration= getEnclosingMethodDeclaration(creation);
			if (declaration != null) {
				ITypeBinding binding= null;
				TypeParameter parameter= null;
				for (final Iterator iterator= declaration.typeParameters().iterator(); iterator.hasNext();) {
					parameter= (TypeParameter) iterator.next();
					binding= parameter.resolveBinding();
					if (binding != null)
						parameters.add(binding);
				}
			}
		}
		final TypeVariableFinder finder= new TypeVariableFinder();
		creation.accept(finder);
		final ITypeBinding[] variables= finder.getResult();
		final List remove= new ArrayList(4);
		boolean match= false;
		ITypeBinding binding= null;
		ITypeBinding variable= null;
		for (final Iterator iterator= parameters.iterator(); iterator.hasNext();) {
			match= false;
			binding= (ITypeBinding) iterator.next();
			for (int index= 0; index < variables.length; index++) {
				variable= variables[index];
				if (variable.equals(binding))
					match= true;
			}
			if (!match)
				remove.add(binding);
		}
		parameters.removeAll(remove);
		final ITypeBinding[] result= new ITypeBinding[parameters.size()];
		parameters.toArray(result);
		return result;
	}

	private MethodDeclaration getEnclosingMethodDeclaration(ASTNode node) {
		ASTNode parent= node.getParent();
		if (parent != null) {
			if (parent instanceof AbstractTypeDeclaration)
				return null;
			else if (parent instanceof MethodDeclaration)
				return (MethodDeclaration) parent;
			return getEnclosingMethodDeclaration(parent);
		}
		return null;
	}

	private Change createChange(CompilationUnitRewrite rewrite) throws CoreException {
		final ITypeBinding binding= fAnonymousInnerClassNode.resolveBinding();
		final String[] labels= new String[] { BindingLabelProvider.getBindingLabel(binding, JavaElementLabels.ALL_FULLY_QUALIFIED), BindingLabelProvider.getBindingLabel(binding.getDeclaringMethod(), JavaElementLabels.ALL_FULLY_QUALIFIED)};
		final Map arguments= new HashMap();
		String project= null;
		IJavaProject javaProject= fCu.getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		final int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | JavaRefactoringDescriptor.JAR_REFACTORABLE | JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		final String description= RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_descriptor_description_short;
		final String header= Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_descriptor_description, labels);
		final JavaRefactoringDescriptorComment comment= new JavaRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_original_pattern, BindingLabelProvider.getBindingLabel(binding, JavaElementLabels.ALL_FULLY_QUALIFIED)));
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_class_name_pattern, fClassName));
		String visibility= JdtFlags.getVisibilityString(fVisibility);
		if ("".equals(visibility)) //$NON-NLS-1$
			visibility= RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_default_visibility;
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_visibility_pattern, visibility));
		if (fDeclareFinal && fDeclareStatic)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_final_static);
		else if (fDeclareFinal)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_final);			
		else if (fDeclareStatic)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_static);			
		final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(IJavaRefactorings.CONVERT_ANONYMOUS, project, description, comment.asString(), arguments, flags);
		arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCu));
		arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_NAME, fClassName);
		arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_SELECTION, new Integer(fSelectionStart).toString() + " " + new Integer(fSelectionLength).toString()); //$NON-NLS-1$
		arguments.put(ATTRIBUTE_FINAL, Boolean.valueOf(fDeclareFinal).toString());
		arguments.put(ATTRIBUTE_STATIC, Boolean.valueOf(fDeclareStatic).toString());
		arguments.put(ATTRIBUTE_VISIBILITY, new Integer(fVisibility).toString());
		final CompilationUnitDescriptorChange result= new CompilationUnitDescriptorChange(descriptor, RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_name, fCu);
		try {
			ITextFileBuffer buffer= RefactoringFileBuffers.acquire(fCu);
			TextEdit resultingEdits= rewrite.getASTRewrite().rewriteAST(buffer.getDocument(), fCu.getJavaProject().getOptions(true));
			TextChangeCompatibility.addTextEdit(result, RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_edit_name, resultingEdits);
		} finally {
			RefactoringFileBuffers.release(fCu);
		}
		return result;
	}

    private void modifyConstructorCall(CompilationUnitRewrite rewrite, ITypeBinding[] parameters) {
        rewrite.getASTRewrite().replace(fAnonymousInnerClassNode.getParent(), createNewClassInstanceCreation(rewrite, parameters), null);
    }

    private ASTNode createNewClassInstanceCreation(CompilationUnitRewrite rewrite, ITypeBinding[] parameters) {
		AST ast= fAnonymousInnerClassNode.getAST();
		ClassInstanceCreation newClassCreation= ast.newClassInstanceCreation();
		newClassCreation.setAnonymousClassDeclaration(null);
		Type type= null;
		if (parameters.length > 0) {
			final ParameterizedType parameterized= ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName(fClassName)));
			for (int index= 0; index < parameters.length; index++)
				parameterized.typeArguments().add(ast.newSimpleType(ast.newSimpleName(parameters[index].getName())));
			type= parameterized;
		} else
			type= ast.newSimpleType(ast.newSimpleName(fClassName));
		newClassCreation.setType(type);
		copyArguments(rewrite, newClassCreation);
		addArgumentsForLocalsUsedInInnerClass(rewrite, newClassCreation);
		return newClassCreation;
	}

    private void addArgumentsForLocalsUsedInInnerClass(CompilationUnitRewrite rewrite, ClassInstanceCreation newClassCreation) {
        IVariableBinding[] usedLocals= getUsedLocalVariables();
        for (int i= 0; i < usedLocals.length; i++) {
            final AST ast= fAnonymousInnerClassNode.getAST();
			final IVariableBinding binding= usedLocals[i];
			Name name= null;
			if (binding.isEnumConstant())
				name= ast.newQualifiedName(ast.newSimpleName(binding.getDeclaringClass().getName()), ast.newSimpleName(binding.getName()));
			else
				name= ast.newSimpleName(binding.getName());
			newClassCreation.arguments().add(name);
        }
    }

    private void copyArguments(CompilationUnitRewrite rewrite, ClassInstanceCreation newClassCreation) {
        for (Iterator iter= ((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).arguments().iterator(); iter.hasNext(); )
            newClassCreation.arguments().add(rewrite.getASTRewrite().createCopyTarget((Expression)iter.next()));
    }

    private void addNestedClass(CompilationUnitRewrite rewrite, ITypeBinding[] parameters) throws CoreException {
        final AbstractTypeDeclaration declarations= (AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
        int index= findIndexOfFistNestedClass(declarations.bodyDeclarations());
        if (index == -1)
            index= 0;
        rewrite.getASTRewrite().getListRewrite(declarations, declarations.getBodyDeclarationsProperty()).insertAt(createNewNestedClass(rewrite, parameters), index, null);
    }

    private static int findIndexOfFistNestedClass(List bodyDeclarations) {
        for (int i= 0, n= bodyDeclarations.size(); i < n; i++) {
            BodyDeclaration each= (BodyDeclaration)bodyDeclarations.get(i);
            if (isNestedType(each))
                return i;
        }
        return -1;
    }

    private static boolean isNestedType(BodyDeclaration each) {
        if (!(each instanceof AbstractTypeDeclaration))
            return false;
        return (each.getParent() instanceof AbstractTypeDeclaration);
    }

    private AbstractTypeDeclaration createNewNestedClass(CompilationUnitRewrite rewrite, ITypeBinding[] parameters) throws CoreException {
		final AST ast= fAnonymousInnerClassNode.getAST();
		final TypeDeclaration declaration= ast.newTypeDeclaration();
		declaration.setInterface(false);
		declaration.setJavadoc(null);
		declaration.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiersForNestedClass()));
		declaration.setName(ast.newSimpleName(fClassName));
		TypeParameter parameter= null;
		for (int index= 0; index < parameters.length; index++) {
			parameter= ast.newTypeParameter();
			parameter.setName(ast.newSimpleName(parameters[index].getName()));
			declaration.typeParameters().add(parameter);
		}
		setSuperType(declaration);
		removeInitializationFromDeclaredFields(rewrite);
		final String delimiter= StubUtility.getLineDelimiterUsed(fCu);
		ITextFileBuffer buffer= null;
		try {
			buffer= RefactoringFileBuffers.acquire(fCu);
			final IDocument document= buffer.getDocument();
			IBinding[] bindings= getUsedLocalVariables();
			for (final Iterator iterator= fAnonymousInnerClassNode.bodyDeclarations().iterator(); iterator.hasNext();) {
				final BodyDeclaration body= (BodyDeclaration) iterator.next();
				declaration.bodyDeclarations().add(createBodyDeclaration(rewrite, document, bindings, body, delimiter));
			}
		} finally {
			if (buffer != null)
				RefactoringFileBuffers.release(fCu);
		}
		createFieldsForAccessedLocals(rewrite, declaration);
		createNewConstructorIfNeeded(rewrite, declaration);
		if (fSettings.createComments) {
			String[] parameterNames= new String[parameters.length];
			for (int index= 0; index < parameterNames.length; index++) {
				parameterNames[index]= parameters[index].getName();
			}
			String string= CodeGeneration.getTypeComment(rewrite.getCu(), fClassName, parameterNames, StubUtility.getLineDelimiterUsed(rewrite.getCu()));
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.getASTRewrite().createStringPlaceholder(string, ASTNode.JAVADOC);
				declaration.setJavadoc(javadoc);
			}
		}
		return declaration;
	}

	private ASTNode createBodyDeclaration(CompilationUnitRewrite rewriter, IDocument document, IBinding[] bindings, BodyDeclaration body, String delimiter) {
		final ASTRewrite rewrite= ASTRewrite.create(rewriter.getAST());
		final ITrackedNodePosition position= rewrite.track(body);
		final IBinding[] binding= { null};
		final ASTNode[] newNode= { null};
		List excludedFields= new ArrayList();
		final AST ast= fAnonymousInnerClassNode.getAST();
		final IJavaProject javaProject= fCu.getJavaProject();
		final IJavaProject project= javaProject;
		for (int index= 0; index < bindings.length; index++) {
			binding[0]= bindings[index];
			String name= binding[0].getName();
			String fieldName= name;
			String oldName= name;
			if (binding[0] instanceof IVariableBinding) {
				IVariableBinding variable= (IVariableBinding) binding[0];
				if (!variable.isEnumConstant()) {
					name= NamingConventions.removePrefixAndSuffixForLocalVariableName(project, name);
					if (name.equals(oldName))
						name= NamingConventions.removePrefixAndSuffixForArgumentName(project, name);
					fieldName= NamingConventions.suggestFieldNames(project, "", name, 0, Flags.AccPrivate, (String[]) excludedFields.toArray(new String[excludedFields.size()]))[0]; //$NON-NLS-1$
					excludedFields.add(fieldName);
					if (fSettings.useKeywordThis) {
						FieldAccess access= ast.newFieldAccess();
						access.setExpression(ast.newThisExpression());
						access.setName(ast.newSimpleName(fieldName));
						newNode[0]= access;
					} else
						newNode[0]= ast.newSimpleName(fieldName);
				} else
					newNode[0]= ast.newSimpleName(fieldName);
			}
			body.accept(new ASTVisitor() {

				public boolean visit(SimpleName node) {
					IBinding resolved= node.resolveBinding();
					if (binding[0].equals(resolved))
						rewrite.replace(node, ASTNode.copySubtree(ast, newNode[0]), null);
					return false;
				}

			});
		}
		final IDocument buffer= new Document(document.get());
		final Map options= javaProject.getOptions(true);
		final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(javaProject);
		final TextEdit edit= rewrite.rewriteAST(buffer, options);
		try {
			edit.apply(buffer, TextEdit.UPDATE_REGIONS);
			final String trimmed= Strings.trimIndentation(buffer.get(position.getStartPosition(), position.getLength()), settings.tabWidth, settings.indentWidth, false);
			return rewriter.getASTRewrite().createStringPlaceholder(trimmed, ASTNode.METHOD_DECLARATION);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return null;
	}

	private void removeInitializationFromDeclaredFields(CompilationUnitRewrite rewrite) {
		for (Iterator iter= getFieldsToInitializeInConstructor().iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			Assert.isNotNull(fragment.getInitializer());
			rewrite.getASTRewrite().remove(fragment.getInitializer(), null);
		}
	}

    private void createFieldsForAccessedLocals(CompilationUnitRewrite rewrite, AbstractTypeDeclaration declaration) {
		final IVariableBinding[] bindings= getUsedLocalVariables();
		final IJavaProject project= rewrite.getCu().getJavaProject();
		List excluded= new ArrayList();
		final AST ast= fAnonymousInnerClassNode.getAST();
		for (int index= 0; index < bindings.length; index++) {
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setExtraDimensions(0);
			fragment.setInitializer(null);
			String name= bindings[index].getName();
			String oldName= name;
			name= NamingConventions.removePrefixAndSuffixForLocalVariableName(project, name);
			if (name.equals(oldName))
				name= NamingConventions.removePrefixAndSuffixForArgumentName(project, name);
			name= NamingConventions.suggestFieldNames(project, "", name, 0, Flags.AccPrivate, (String[]) excluded.toArray(new String[excluded.size()]))[0]; //$NON-NLS-1$
			fragment.setName(ast.newSimpleName(name));
			excluded.add(name);
			FieldDeclaration field= ast.newFieldDeclaration(fragment);
			field.setType(rewrite.getImportRewrite().addImport(bindings[index].getType(), ast));
			field.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.PRIVATE | Modifier.FINAL));
			declaration.bodyDeclarations().add(findIndexOfLastField(declaration.bodyDeclarations()) + 1, field);
			if (fSettings.createComments) {
				try {
					String string= CodeGeneration.getFieldComment(rewrite.getCu(), bindings[index].getType().getName(), name, StubUtility.getLineDelimiterUsed(rewrite.getCu()));
					if (string != null) {
						Javadoc javadoc= (Javadoc) rewrite.getASTRewrite().createStringPlaceholder(string, ASTNode.JAVADOC);
						field.setJavadoc(javadoc);
					}
				} catch (CoreException exception) {
					JavaPlugin.log(exception);
				}
			}
		}
	}

    private IVariableBinding[] getUsedLocalVariables() {
        final Set result= new HashSet(0);
        fAnonymousInnerClassNode.accept(createTempUsageFinder(result));
        return (IVariableBinding[])result.toArray(new IVariableBinding[result.size()]);
    }

    private ASTVisitor createTempUsageFinder(final Set result) {
        return new ASTVisitor() {
            public boolean visit(SimpleName node) {
                IBinding binding= node.resolveBinding();
                if (ConvertAnonymousToNestedRefactoring.this.isBindingToTemp(binding))
                    result.add(binding);
                return true;
            }
        };
    }

    private boolean isBindingToTemp(IBinding binding) {
		if (!(binding instanceof IVariableBinding))
			return false;
		final IVariableBinding variable= (IVariableBinding) binding;
		if (variable.isField())
			return false;
		if (!Modifier.isFinal(binding.getModifiers()))
			return false;
		ASTNode declaringNode= fCompilationUnitNode.findDeclaringNode(binding);
		if (declaringNode == null)
			return false;
		if (ASTNodes.isParent(declaringNode, fAnonymousInnerClassNode))
			return false;
		return true;
	}

    private void createNewConstructorIfNeeded(CompilationUnitRewrite rewrite, AbstractTypeDeclaration declaration) throws JavaModelException {
		IVariableBinding[] bindings= getUsedLocalVariables();

		if (((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).arguments().isEmpty() && bindings.length == 0)
			return;

		final AST ast= fAnonymousInnerClassNode.getAST();
		MethodDeclaration newConstructor= ast.newMethodDeclaration();
		newConstructor.setConstructor(true);
		newConstructor.setExtraDimensions(0);
		newConstructor.setJavadoc(null);
		newConstructor.modifiers().addAll(ASTNodeFactory.newModifiers(ast, fVisibility));
		newConstructor.setName(ast.newSimpleName(fClassName));
		List paramNames= new ArrayList();
		addParametersToNewConstructor(newConstructor, rewrite, paramNames);
		int paramCount= newConstructor.parameters().size();

		addParametersForLocalsUsedInInnerClass(rewrite, bindings, newConstructor, paramNames);

		Block body= ast.newBlock();
		if (paramCount > 0) {
			SuperConstructorInvocation superConstructorInvocation= ast.newSuperConstructorInvocation();
			for (int i= 0; i < paramCount; i++) {
				SingleVariableDeclaration param= (SingleVariableDeclaration) newConstructor.parameters().get(i);
				superConstructorInvocation.arguments().add(ast.newSimpleName(param.getName().getIdentifier()));
			}
			body.statements().add(superConstructorInvocation);
		}
		final IJavaProject project= fCu.getJavaProject();
		List excludedFields= new ArrayList();
		List excludedParams= new ArrayList();
		for (int index= 0; index < bindings.length; index++) {
			String name= bindings[index].getName();
			String fieldName= name;
			String paramName= name;
			String oldName= name;
			name= NamingConventions.removePrefixAndSuffixForLocalVariableName(project, name);
			if (name.equals(oldName))
				name= NamingConventions.removePrefixAndSuffixForArgumentName(project, name);
			fieldName= NamingConventions.suggestFieldNames(project, "", name, 0, Flags.AccPrivate, (String[]) excludedFields.toArray(new String[excludedFields.size()]))[0]; //$NON-NLS-1$
			excludedFields.add(fieldName);
			paramName= NamingConventions.suggestArgumentNames(project, "", name, 0, (String[]) excludedParams.toArray(new String[excludedParams.size()]))[0]; //$NON-NLS-1$
			excludedParams.add(paramName);
			Assignment assignmentExpression= ast.newAssignment();
			assignmentExpression.setOperator(Assignment.Operator.ASSIGN);
			if (fSettings.useKeywordThis || fieldName.equals(paramName)) {
				FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(fieldName));
				assignmentExpression.setLeftHandSide(access);
			} else
				assignmentExpression.setLeftHandSide(ast.newSimpleName(fieldName));
			assignmentExpression.setRightHandSide(ast.newSimpleName(paramName));
			ExpressionStatement assignmentStatement= ast.newExpressionStatement(assignmentExpression);
			body.statements().add(assignmentStatement);
		}

		addFieldInitialization(rewrite, body);

		newConstructor.setBody(body);

		addExceptionsToNewConstructor(newConstructor);
		declaration.bodyDeclarations().add(1 + bindings.length + findIndexOfLastField(fAnonymousInnerClassNode.bodyDeclarations()), newConstructor);
		if (fSettings.createComments) {
			try {
				String string= CodeGeneration.getMethodComment(rewrite.getCu(), fClassName, fClassName, (String[]) paramNames.toArray(new String[paramNames.size()]), new String[0], null, new String[0], null, StubUtility.getLineDelimiterUsed(rewrite.getCu()));
				if (string != null) {
					Javadoc javadoc= (Javadoc) rewrite.getASTRewrite().createStringPlaceholder(string, ASTNode.JAVADOC);
					newConstructor.setJavadoc(javadoc);
				}
			} catch (CoreException exception) {
				JavaPlugin.log(exception);
			}
		}
	}

    private void addFieldInitialization(CompilationUnitRewrite rewrite, Block constructorBody) {
		final IJavaProject project= rewrite.getCu().getJavaProject();
		List excluded= new ArrayList();
		final AST ast= fAnonymousInnerClassNode.getAST();
		for (Iterator iter= getFieldsToInitializeInConstructor().iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			Assignment assignmentExpression= ast.newAssignment();
			assignmentExpression.setOperator(Assignment.Operator.ASSIGN);
			String name= fragment.getName().getIdentifier();
			String oldName= name;
			name= NamingConventions.removePrefixAndSuffixForLocalVariableName(project, name);
			if (name.equals(oldName))
				name= NamingConventions.removePrefixAndSuffixForArgumentName(project, name);
			name= NamingConventions.suggestFieldNames(project, "", name, 0, Flags.AccPrivate, (String[]) excluded.toArray(new String[excluded.size()]))[0]; //$NON-NLS-1$
			if (fSettings.useKeywordThis) {
				FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(name));
				assignmentExpression.setLeftHandSide(access);
			} else
				assignmentExpression.setLeftHandSide(ast.newSimpleName(name));
			excluded.add(name);
			Expression rhs= (Expression) rewrite.getASTRewrite().createCopyTarget(fragment.getInitializer());
			assignmentExpression.setRightHandSide(rhs);
			ExpressionStatement assignmentStatement= ast.newExpressionStatement(assignmentExpression);
			constructorBody.statements().add(assignmentStatement);
		}
	}

    // live List of VariableDeclarationFragments
    private List getFieldsToInitializeInConstructor() {
        List result= new ArrayList(0);
        for (Iterator iter= fAnonymousInnerClassNode.bodyDeclarations().iterator(); iter.hasNext(); ) {
            BodyDeclaration element= (BodyDeclaration)iter.next();
            if (!(element instanceof FieldDeclaration))
                continue;
            FieldDeclaration field= (FieldDeclaration)element;
            for (Iterator fragmentIter= field.fragments().iterator(); fragmentIter.hasNext(); ) {
                VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragmentIter.next();
                if (isToBeInitializerInConstructor(fragment))
                    result.add(fragment);
            }
        }
        return result;
    }

    private boolean isToBeInitializerInConstructor(VariableDeclarationFragment fragment) {
        if (fragment.getInitializer() == null)
            return false;
        return areLocalsUsedIn(fragment.getInitializer());
    }

    private boolean areLocalsUsedIn(Expression fieldInitializer) {
        Set localsUsed= new HashSet(0);
        fieldInitializer.accept(createTempUsageFinder(localsUsed));
        return !localsUsed.isEmpty();
    }

    private void addParametersForLocalsUsedInInnerClass(CompilationUnitRewrite rewrite, IVariableBinding[] usedLocals, MethodDeclaration newConstructor, List params) {
    	List excluded= new ArrayList();
        for (int index= 0; index < usedLocals.length; index++) {
        	SingleVariableDeclaration declaration= createNewParamDeclarationNode(usedLocals[index].getName(), usedLocals[index].getType(), rewrite, (String[]) excluded.toArray(new String[excluded.size()]));
            newConstructor.parameters().add(declaration);
            final String identifier= declaration.getName().getIdentifier();
			excluded.add(identifier);
			params.add(identifier);
        }
    }

    private IMethodBinding getSuperConstructorBinding() {
        //workaround for missing java core functionality - finding a
        // super constructor for an anonymous class creation
        IMethodBinding anonConstr= ((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).resolveConstructorBinding();
        if (anonConstr == null)
            return null;
        ITypeBinding superClass= anonConstr.getDeclaringClass().getSuperclass();
        IMethodBinding[] superMethods= superClass.getDeclaredMethods();
        for (int i= 0; i < superMethods.length; i++) {
            IMethodBinding superMethod= superMethods[i];
            if (superMethod.isConstructor() && parameterTypesMatch(superMethod, anonConstr))
                return superMethod;
        }
        Assert.isTrue(false);//there's no way - it must be there
        return null;
    }

    private static boolean parameterTypesMatch(IMethodBinding m1, IMethodBinding m2) {
        ITypeBinding[] m1Params= m1.getParameterTypes();
        ITypeBinding[] m2Params= m2.getParameterTypes();
        if (m1Params.length != m2Params.length)
            return false;
        for (int i= 0; i < m2Params.length; i++) {
            if (!m1Params[i].equals(m2Params[i]))
                return false;
        }
        return true;
    }

    private void addExceptionsToNewConstructor(MethodDeclaration newConstructor) {
        IMethodBinding constructorBinding= getSuperConstructorBinding();
        if (constructorBinding == null)
            return;
        ITypeBinding[] exceptions= constructorBinding.getExceptionTypes();
        for (int i= 0; i < exceptions.length; i++) {
            Name exceptionTypeName= fAnonymousInnerClassNode.getAST().newName(Bindings.getNameComponents(exceptions[i]));
            newConstructor.thrownExceptions().add(exceptionTypeName);
        }
    }

    private void addParametersToNewConstructor(MethodDeclaration newConstructor, CompilationUnitRewrite rewrite, List params) throws JavaModelException {
        IMethodBinding constructorBinding= getSuperConstructorBinding();
        if (constructorBinding == null)
            return;
        ITypeBinding[] paramTypes= constructorBinding.getParameterTypes();
        IMethod method= (IMethod) constructorBinding.getJavaElement();
        if (method == null)
            return;
        String[] parameterNames= method.getParameterNames();
        final List excluded= new ArrayList();
        for (int index= 0; index < parameterNames.length; index++) {
        	SingleVariableDeclaration declaration= createNewParamDeclarationNode(parameterNames[index], paramTypes[index], rewrite, (String[]) excluded.toArray(new String[excluded.size()]));
            newConstructor.parameters().add(declaration);
            final String identifier= declaration.getName().getIdentifier();
			excluded.add(identifier);
			params.add(identifier);
        }
    }

    private SingleVariableDeclaration createNewParamDeclarationNode(String paramName, ITypeBinding paramType, CompilationUnitRewrite rewrite, String[] excluded) {
		SingleVariableDeclaration param= fAnonymousInnerClassNode.getAST().newSingleVariableDeclaration();
		param.setExtraDimensions(0);
		param.setInitializer(null);
		final IJavaProject project= rewrite.getCu().getJavaProject();
		String name= NamingConventions.removePrefixAndSuffixForLocalVariableName(project, paramName);
		if (name.equals(paramName))
			name= NamingConventions.removePrefixAndSuffixForArgumentName(project, paramName);
		name= NamingConventions.suggestArgumentNames(project, "", name, 0, excluded)[0]; //$NON-NLS-1$
		param.setName(fAnonymousInnerClassNode.getAST().newSimpleName(name));
		param.setType(rewrite.getImportRewrite().addImport(paramType, fAnonymousInnerClassNode.getAST()));
		return param;
	}

    private void setSuperType(TypeDeclaration declaration) throws JavaModelException {
        ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation) fAnonymousInnerClassNode.getParent();
		ITypeBinding binding= classInstanceCreation.resolveTypeBinding();
        if (binding == null)
            return;
		Type newType= (Type) ASTNode.copySubtree(fAnonymousInnerClassNode.getAST(), classInstanceCreation.getType());
		if (binding.getSuperclass().getQualifiedName().equals("java.lang.Object")) { //$NON-NLS-1$
            Assert.isTrue(binding.getInterfaces().length <= 1);
            if (binding.getInterfaces().length == 0)
                return;
            declaration.superInterfaceTypes().add(0, newType); 
        } else {
            declaration.setSuperclassType(newType); 
        }
    }

    private ITypeBinding getSuperTypeBinding() {
    	ITypeBinding types= fAnonymousInnerClassNode.resolveBinding();
    	ITypeBinding[] interfaces= types.getInterfaces();
    	if (interfaces.length > 0)
    		return interfaces[0];
    	else
    		return types.getSuperclass();
    }

    private int createModifiersForNestedClass() {
        int flags= fVisibility;
        if (fDeclareFinal)
            flags|= Modifier.FINAL;
        if (mustInnerClassBeStatic() || fDeclareStatic)
            flags|= Modifier.STATIC;
        return flags;
    }

    public boolean mustInnerClassBeStatic() {
        ITypeBinding typeBinding = ((AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class)).resolveBinding();
        ASTNode current = fAnonymousInnerClassNode.getParent();
        boolean ans = false;
        while(current != null) {
            switch(current.getNodeType()) {
                case ASTNode.ANONYMOUS_CLASS_DECLARATION:
                {
                    AnonymousClassDeclaration enclosingAnonymousClassDeclaration= (AnonymousClassDeclaration)current;
                    ITypeBinding binding= enclosingAnonymousClassDeclaration.resolveBinding();
                    if (binding != null && Bindings.isSuperType(typeBinding, binding.getSuperclass())) {
                        return false;
                    }
                    break;
                }
                case ASTNode.FIELD_DECLARATION:
                {
                    FieldDeclaration enclosingFieldDeclaration= (FieldDeclaration)current;
                    if (Modifier.isStatic(enclosingFieldDeclaration.getModifiers())) {
                        ans = true;
                    }
                    break;
                }
                case ASTNode.METHOD_DECLARATION:
                {
                    MethodDeclaration enclosingMethodDeclaration = (MethodDeclaration)current;
                    if (Modifier.isStatic(enclosingMethodDeclaration.getModifiers())) {
                        ans = true;
                    }
                    break;
                }
                case ASTNode.TYPE_DECLARATION:
                {
                    return ans;
                }
            }
            current = current.getParent();
        }
        return ans;
    }

    private static int findIndexOfLastField(List bodyDeclarations) {
        for (int i= bodyDeclarations.size() - 1; i >= 0; i--) {
            BodyDeclaration each= (BodyDeclaration)bodyDeclarations.get(i);
            if (each instanceof FieldDeclaration)
                return i;
        }
        return -1;
    }

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		fSelfInitializing= true;
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaElement element= JavaRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaElement.COMPILATION_UNIT)
					return createInputFatalStatus(element, IJavaRefactorings.CONVERT_ANONYMOUS);
				else {
					fCu= (ICompilationUnit) element;
		        	fSettings= JavaPreferencesSettings.getCodeGenerationSettings(fCu.getJavaProject());
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fClassName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptor.ATTRIBUTE_NAME));
			final String visibility= extended.getAttribute(ATTRIBUTE_VISIBILITY);
			if (visibility != null && !"".equals(visibility)) {//$NON-NLS-1$
				int flag= 0;
				try {
					flag= Integer.parseInt(visibility);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_VISIBILITY));
				}
				fVisibility= flag;
			}
			final String selection= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_SELECTION);
			if (selection != null) {
				int offset= -1;
				int length= -1;
				final StringTokenizer tokenizer= new StringTokenizer(selection);
				if (tokenizer.hasMoreTokens())
					offset= Integer.valueOf(tokenizer.nextToken()).intValue();
				if (tokenizer.hasMoreTokens())
					length= Integer.valueOf(tokenizer.nextToken()).intValue();
				if (offset >= 0 && length >= 0) {
					fSelectionStart= offset;
					fSelectionLength= length;
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection, JavaRefactoringDescriptor.ATTRIBUTE_SELECTION}));
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptor.ATTRIBUTE_SELECTION));
			final String declareStatic= extended.getAttribute(ATTRIBUTE_STATIC);
			if (declareStatic != null) {
				fDeclareStatic= Boolean.valueOf(declareStatic).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STATIC));
			final String declareFinal= extended.getAttribute(ATTRIBUTE_FINAL);
			if (declareFinal != null) {
				fDeclareFinal= Boolean.valueOf(declareStatic).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FINAL));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
