/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
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
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

public class ConvertAnonymousToNestedRefactoring extends Refactoring {

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

    private final int fSelectionStart;
    private final int fSelectionLength;
    private final ICompilationUnit fCu;

    private int fVisibility; /* see Modifier */
    private boolean fDeclareFinal;
    private boolean fDeclareStatic;
    private String fClassName;

    private CompilationUnit fCompilationUnitNode;
    private AnonymousClassDeclaration fAnonymousInnerClassNode;
    private Set fClassNamesUsed;

    private ConvertAnonymousToNestedRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
        Assert.isTrue(selectionStart >= 0);
        Assert.isTrue(selectionLength >= 0);
        Assert.isTrue(cu.exists());
        fSelectionStart= selectionStart;
        fSelectionLength= selectionLength;
        fCu= cu;
    }
    
    public static boolean isAvailable(IType type) throws JavaModelException {
    	return type.isAnonymous();
    }

    public static ConvertAnonymousToNestedRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength) {
        return new ConvertAnonymousToNestedRefactoring(cu, selectionStart, selectionLength);
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
    
    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
     */
    public String getName() {
        return RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.name"); //$NON-NLS-1$
    }

    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
     */
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        RefactoringStatus result= Checks.validateModifiesFiles(
        	ResourceUtil.getFiles(new ICompilationUnit[]{fCu}),
			getValidationContext());
		if (result.hasFatalError())
		    return result;

		initAST(pm);

		if (fAnonymousInnerClassNode == null)
		    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.place_caret")); //$NON-NLS-1$
		initializeDefaults();
		if (getSuperConstructorBinding() == null)
		    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.compile_errors")); //$NON-NLS-1$
		if (getSuperTypeBinding().isLocal())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.extends_local_class")); //$NON-NLS-1$
		return new RefactoringStatus();
    }

    private void initializeDefaults() {
        fVisibility= isLocalInnerType() ? Modifier.NONE : Modifier.PRIVATE;
        fClassName= ""; //$NON-NLS-1$
        fDeclareFinal= true;
        fDeclareStatic = mustInnerClassBeStatic();
    }

    private void initAST(IProgressMonitor pm) {
		fCompilationUnitNode= new RefactoringASTParser(AST.JLS3).parse(fCu, true, pm);
		fAnonymousInnerClassNode= getAnonymousInnerClass(NodeFinder.perform(fCompilationUnitNode, fSelectionStart, fSelectionLength));
		if (fAnonymousInnerClassNode != null) {
			final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
			if (declaration instanceof TypeDeclaration) {
				final AbstractTypeDeclaration[] nested= ((TypeDeclaration) declaration).getTypes();
				fClassNamesUsed= new HashSet(nested.length);
				for (int index= 0; index < nested.length; index++)
					fClassNamesUsed.add(nested[index].getName().getIdentifier());
			}
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
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.type_exists")); //$NON-NLS-1$
        IMethodBinding superConstructorBinding = getSuperConstructorBinding();
        if (superConstructorBinding == null)
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.compile_errors")); //$NON-NLS-1$
        if (fClassName.equals(superConstructorBinding.getDeclaringClass().getName()))
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.another_name")); //$NON-NLS-1$
        if (classNameHidesEnclosingType())
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.name_hides")); //$NON-NLS-1$
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
            public boolean visit(SimpleName node) {
                final IBinding binding = node.resolveBinding();
                if(binding != null && binding instanceof IVariableBinding)
                    accessedFields.add(binding);
                return super.visit(node);
            }

            public boolean visit(FieldAccess node) {
                final IVariableBinding binding = node.resolveFieldBinding();
                if(binding != null)
                    accessedFields.add(binding);
                return super.visit(node);
            }
            
            public boolean visit(QualifiedName node) {
                final IBinding binding = node.resolveBinding();
                if(binding != null && binding instanceof IVariableBinding)
                    accessedFields.add(binding);
                return super.visit(node);
            }
            
            public boolean visit(SuperFieldAccess node) {
                final IVariableBinding binding = node.resolveFieldBinding();
                if(binding != null)
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
                status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.anonymous_field_access"))); //$NON-NLS-1$
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
			final ASTRewrite rewrite= ASTRewrite.create(fCompilationUnitNode.getAST());
			final ITypeBinding[] parameters= getTypeParameters();
			addNestedClass(rewrite, parameters);
			modifyConstructorCall(rewrite, parameters);
			return createChange(rewrite);
		} finally {
			pm.done();
		}
	}

	private ITypeBinding[] getTypeParameters() {
		final List list= new ArrayList();
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
					binding= (ITypeBinding) parameter.resolveBinding();
					if (binding != null)
						list.add(binding);
				}
			}
		}
		final ITypeBinding[] result= new ITypeBinding[list.size()];
		list.toArray(result);
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

	private Change createChange(ASTRewrite rewrite) throws CoreException {
		TextChange change= new CompilationUnitChange("", fCu); //$NON-NLS-1$
		try {
			ITextFileBuffer buffer= RefactoringFileBuffers.acquire(fCu);
			TextEdit resultingEdits= rewrite.rewriteAST(buffer.getDocument(), fCu.getJavaProject().getOptions(true));
			TextChangeCompatibility.addTextEdit(change, RefactoringCoreMessages.getString("ConvertAnonymousToNestedRefactoring.edit_name"), resultingEdits); //$NON-NLS-1$
		} finally {
			RefactoringFileBuffers.release(fCu);
		}
		return change;
	}

    private void modifyConstructorCall(ASTRewrite rewrite, ITypeBinding[] parameters) {
        rewrite.replace(fAnonymousInnerClassNode.getParent(), createNewClassInstanceCreation(rewrite, parameters), null);
    }

    private ASTNode createNewClassInstanceCreation(ASTRewrite rewrite, ITypeBinding[] parameters) {
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

    private void addArgumentsForLocalsUsedInInnerClass(ASTRewrite rewrite, ClassInstanceCreation newClassCreation) {
        IVariableBinding[] usedLocals= getUsedLocalVariables();
        for (int i= 0; i < usedLocals.length; i++)
            newClassCreation.arguments().add(fAnonymousInnerClassNode.getAST().newSimpleName(usedLocals[i].getName()));
    }

    private void copyArguments(ASTRewrite rewrite, ClassInstanceCreation newClassCreation) {
        for (Iterator iter= ((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).arguments().iterator(); iter.hasNext(); )
            newClassCreation.arguments().add(rewrite.createCopyTarget((Expression)iter.next()));
    }

    private void addNestedClass(ASTRewrite rewrite, ITypeBinding[] parameters) throws JavaModelException {
        final AbstractTypeDeclaration declarations= (AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
        int index= findIndexOfFistNestedClass(declarations.bodyDeclarations());
        if (index == -1)
            index= 0;
        rewrite.getListRewrite(declarations, declarations.getBodyDeclarationsProperty()).insertAt(createNewNestedClass(rewrite, parameters), index, null);
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

    private AbstractTypeDeclaration createNewNestedClass(ASTRewrite rewrite, ITypeBinding[] parameters) throws JavaModelException {
        final TypeDeclaration declaration= fAnonymousInnerClassNode.getAST().newTypeDeclaration();
        declaration.setInterface(false);
        declaration.setJavadoc(null);
        declaration.modifiers().addAll(ASTNodeFactory.newModifiers(fAnonymousInnerClassNode.getAST(), createModifiersForNestedClass()));
        declaration.setName(fAnonymousInnerClassNode.getAST().newSimpleName(fClassName));
        TypeParameter parameter= null;
        for (int index= 0; index < parameters.length; index++) {
        	parameter= fAnonymousInnerClassNode.getAST().newTypeParameter();
        	parameter.setName(fAnonymousInnerClassNode.getAST().newSimpleName(parameters[index].getName()));
        	declaration.typeParameters().add(parameter);
        }
        setSuperType(declaration);
        removeInitializationFromDeclaredFields(rewrite);
        for (final Iterator iterator= fAnonymousInnerClassNode.bodyDeclarations().iterator(); iterator.hasNext(); )
		declaration.bodyDeclarations().add(rewrite.createCopyTarget((BodyDeclaration)iterator.next()));
        createFieldsForAccessedLocals(rewrite, declaration);
        createNewConstructorIfNeeded(rewrite, declaration);
        return declaration;
    }

    private void removeInitializationFromDeclaredFields(ASTRewrite rewrite) {
        for (Iterator iter= getFieldsToInitializeInConstructor().iterator(); iter.hasNext(); ) {
            VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
            Assert.isNotNull(fragment.getInitializer());
            rewrite.remove(fragment.getInitializer(), null);
        }
    }

    private void createFieldsForAccessedLocals(ASTRewrite rewrite, AbstractTypeDeclaration declaration) {
        final IVariableBinding[] bindings= getUsedLocalVariables();
        for (int index= 0; index < bindings.length; index++) {
            VariableDeclarationFragment fragment= fAnonymousInnerClassNode.getAST().newVariableDeclarationFragment();
            fragment.setExtraDimensions(0);
            fragment.setInitializer(null);
            fragment.setName(fAnonymousInnerClassNode.getAST().newSimpleName(bindings[index].getName()));
            FieldDeclaration field= fAnonymousInnerClassNode.getAST().newFieldDeclaration(fragment);
            field.setType(ASTNodeFactory.newType(fAnonymousInnerClassNode.getAST(), bindings[index].getType(), false));
            field.modifiers().addAll(ASTNodeFactory.newModifiers(fAnonymousInnerClassNode.getAST(), Modifier.PRIVATE | Modifier.FINAL));
            declaration.bodyDeclarations().add(findIndexOfLastField(declaration.bodyDeclarations()) + 1, field);
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
        if (!Modifier.isFinal(binding.getModifiers()))
            return false;
        ASTNode declaringNode= fCompilationUnitNode.findDeclaringNode(binding);
        if (declaringNode == null)
            return false;
        if (ASTNodes.isParent(declaringNode, fAnonymousInnerClassNode))
            return false;
        return true;
    }

    private void createNewConstructorIfNeeded(ASTRewrite rewrite, AbstractTypeDeclaration declaration) throws JavaModelException {
        IVariableBinding[] bindings= getUsedLocalVariables();

        if (((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).arguments().isEmpty() && bindings.length == 0)
            return;

        MethodDeclaration newConstructor= fAnonymousInnerClassNode.getAST().newMethodDeclaration();
        newConstructor.setConstructor(true);
        newConstructor.setExtraDimensions(0);
        newConstructor.setJavadoc(null);
        newConstructor.modifiers().addAll(ASTNodeFactory.newModifiers(fAnonymousInnerClassNode.getAST(), fVisibility));
        newConstructor.setName(fAnonymousInnerClassNode.getAST().newSimpleName(fClassName));
        addParametersToNewConstructor(newConstructor, rewrite);
        int paramCount= newConstructor.parameters().size();

        addParametersForLocalsUsedInInnerClass(rewrite, bindings, newConstructor);

        Block body= fAnonymousInnerClassNode.getAST().newBlock();
        SuperConstructorInvocation superConstructorInvocation= fAnonymousInnerClassNode.getAST().newSuperConstructorInvocation();
        for (int i= 0; i < paramCount; i++) {
            SingleVariableDeclaration param= (SingleVariableDeclaration)newConstructor.parameters().get(i);
            superConstructorInvocation.arguments().add(fAnonymousInnerClassNode.getAST().newSimpleName(param.getName().getIdentifier()));
        }
        body.statements().add(superConstructorInvocation);

        Map options= fCu.getJavaProject().getOptions(true);
        for (int index= 0; index < bindings.length; index++) {
            String unformattedAssigmentCode= "this." + bindings[index].getName() + "=" + bindings[index].getName(); //$NON-NLS-1$ //$NON-NLS-2$
            String assignmentCode= CodeFormatterUtil.format(CodeFormatter.K_EXPRESSION, unformattedAssigmentCode, 0, null, getLineSeparator(), options);
            Expression assignmentExpression= (Expression)rewrite.createStringPlaceholder(assignmentCode, ASTNode.METHOD_INVOCATION);
            ExpressionStatement assignmentStatement= fAnonymousInnerClassNode.getAST().newExpressionStatement(assignmentExpression);
            body.statements().add(assignmentStatement);
        }

        addFieldInitialization(rewrite, body);

        newConstructor.setBody(body);

        addExceptionsToNewConstructor(newConstructor);
        declaration.bodyDeclarations().add(1 + bindings.length + findIndexOfLastField(fAnonymousInnerClassNode.bodyDeclarations()), newConstructor);
    }

    private void addFieldInitialization(ASTRewrite rewrite, Block constructorBody) {
        for (Iterator iter= getFieldsToInitializeInConstructor().iterator(); iter.hasNext(); ) {
            VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
            Assignment assignmentExpression= fAnonymousInnerClassNode.getAST().newAssignment();
            assignmentExpression.setOperator(Assignment.Operator.ASSIGN);
            assignmentExpression.setLeftHandSide(fAnonymousInnerClassNode.getAST().newSimpleName(fragment.getName().getIdentifier()));
            Expression rhs= (Expression)rewrite.createCopyTarget(fragment.getInitializer());
            assignmentExpression.setRightHandSide(rhs);
            ExpressionStatement assignmentStatement= fAnonymousInnerClassNode.getAST().newExpressionStatement(assignmentExpression);
            constructorBody.statements().add(assignmentStatement);
        }
    }

    //live List of VariableDeclarationFragments
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

    private void addParametersForLocalsUsedInInnerClass(ASTRewrite rewrite, IVariableBinding[] usedLocals, MethodDeclaration newConstructor) {
        for (int i= 0; i < usedLocals.length; i++)
            newConstructor.parameters().add(createNewParamDeclarationNode(usedLocals[i].getName(), usedLocals[i].getType()));
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

    private void addParametersToNewConstructor(MethodDeclaration newConstructor, ASTRewrite rewrite) throws JavaModelException {
        IMethodBinding constructorBinding= getSuperConstructorBinding();
        if (constructorBinding == null)
            return;
        ITypeBinding[] paramTypes= constructorBinding.getParameterTypes();
        IMethod method= Bindings.findMethod(constructorBinding, fCu.getJavaProject());
        if (method == null)
            return;
        String[] parameterNames= method.getParameterNames();
        for (int i= 0; i < parameterNames.length; i++)
            newConstructor.parameters().add(createNewParamDeclarationNode(parameterNames[i], paramTypes[i]));
    }

    private SingleVariableDeclaration createNewParamDeclarationNode(String paramName, ITypeBinding paramType) {
        SingleVariableDeclaration param= fAnonymousInnerClassNode.getAST().newSingleVariableDeclaration();
        param.setExtraDimensions(0);
        param.setInitializer(null);
        param.setName(fAnonymousInnerClassNode.getAST().newSimpleName(paramName));
        param.setType(ASTNodeFactory.newType(fAnonymousInnerClassNode.getAST(), paramType, false));
        return param;
    }

    private void setSuperType(TypeDeclaration declaration) throws JavaModelException {
        ITypeBinding binding= ((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).resolveTypeBinding();
        if (binding == null)
            return;
        if (binding.getSuperclass().getQualifiedName().equals("java.lang.Object")) { //$NON-NLS-1$
            Assert.isTrue(binding.getInterfaces().length <= 1);
            if (binding.getInterfaces().length == 0)
                return;
            declaration.superInterfaceTypes().add(0, fAnonymousInnerClassNode.getAST().newSimpleType(fAnonymousInnerClassNode.getAST().newName(Strings.splitByToken(getNodeSourceCode(((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).getType()), ".")))); //$NON-NLS-1$
        } else {
            declaration.setSuperclassType(fAnonymousInnerClassNode.getAST().newSimpleType(fAnonymousInnerClassNode.getAST().newName(Strings.splitByToken(getNodeSourceCode(((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).getType()), ".")))); //$NON-NLS-1$
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

    private String getNodeSourceCode(ASTNode node) throws JavaModelException {
        return fCu.getBuffer().getText(node.getStartPosition(), node.getLength());
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

    private String getLineSeparator() {
        try {
            return StubUtility.getLineDelimiterUsed(fCu);
        } catch (JavaModelException e) {
            return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static int findIndexOfLastField(List bodyDeclarations) {
        for (int i= bodyDeclarations.size() - 1; i >= 0; i--) {
            BodyDeclaration each= (BodyDeclaration)bodyDeclarations.get(i);
            if (each instanceof FieldDeclaration)
                return i;
        }
        return -1;
    }
}