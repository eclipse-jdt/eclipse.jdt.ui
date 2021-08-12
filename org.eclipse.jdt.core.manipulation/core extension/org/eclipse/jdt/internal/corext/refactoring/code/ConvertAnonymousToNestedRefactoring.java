/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     NikolayMetchev@gmail.com - contributed fixes for
 *     - convert anonymous to nested should sometimes declare class as static [refactoring]
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=43360)
 *     - Convert anonymous to nested: should show error if field form outer anonymous type is references [refactoring]
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=48282)
 *     - [refactoring][convert anonymous] gets confused with generic methods
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=124978)
 *     - [convert anonymous] Convert Anonymous to nested generates wrong code
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=159917)
 *     Microsoft Corporation - copied to jdt.core.manipulation
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ConvertAnonymousDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroupCore;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.text.correction.ModifierCorrectionSubProcessorCore;

public class ConvertAnonymousToNestedRefactoring extends Refactoring {

	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FINAL= "final"; //$NON-NLS-1$
	private static final String ATTRIBUTE_STATIC= "static"; //$NON-NLS-1$

	private static final String KEY_TYPE_NAME= "type_name"; //$NON-NLS-1$
	private static final String KEY_PARAM_NAME_EXT= "param_name_ext"; //$NON-NLS-1$
	private static final String KEY_PARAM_NAME_CONST= "param_name_const"; //$NON-NLS-1$
	private static final String KEY_FIELD_NAME_EXT= "field_name_ext"; //$NON-NLS-1$

	public static class TypeVariableFinder extends ASTVisitor {

		private final Map<String, ITypeBinding> fBindings= new HashMap<>();
		private final List<ITypeBinding> fFound= new ArrayList<>();

		@Override
		public final boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			final ITypeBinding binding= node.resolveTypeBinding();
			if (binding != null && binding.isTypeVariable() && !fBindings.containsKey(binding.getKey())) {
				fBindings.put(binding.getKey(), binding);
				fFound.add(binding);
			}
			return true;
		}

		@Override
		public final boolean visit(TypeParameter parameter) {
			ITypeBinding binding= parameter.resolveBinding();
			if (binding != null) {
				// don't collect type parameters declared inside the anonymous
				fBindings.put(binding.getKey(), binding);
			}
			return false;
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

    private CompilationUnit fCompilationUnitNode;
    private AnonymousClassDeclaration fAnonymousInnerClassNode;
    private Set<String> fClassNamesUsed;
	private boolean fSelfInitializing= false;

	private LinkedProposalModelCore fLinkedProposalModelCore;

	/**
	 * Creates a new convert anonymous to nested refactoring.
	 *
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart start
	 * @param selectionLength length
	 */
    public ConvertAnonymousToNestedRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength) {
        Assert.isTrue(selectionStart >= 0);
        Assert.isTrue(selectionLength >= 0);
        Assert.isTrue(unit == null || unit.exists());
        fSelectionStart= selectionStart;
        fSelectionLength= selectionLength;
        fCu= unit;
        fAnonymousInnerClassNode= null;
        fCompilationUnitNode= null;
    }

    public ConvertAnonymousToNestedRefactoring(AnonymousClassDeclaration declaration) {
    	Assert.isTrue(declaration != null);

    	ASTNode astRoot= declaration.getRoot();
    	Assert.isTrue(astRoot instanceof CompilationUnit);
    	fCompilationUnitNode= (CompilationUnit) astRoot;

     	IJavaElement javaElement= fCompilationUnitNode.getJavaElement();
        Assert.isTrue(javaElement instanceof ICompilationUnit);

        fCu= (ICompilationUnit) javaElement;
        fSelectionStart= declaration.getStartPosition();
        fSelectionLength= declaration.getLength();
    }

    public ConvertAnonymousToNestedRefactoring(JavaRefactoringArguments arguments, RefactoringStatus status) {
   		this(null, 0, 0);
   		RefactoringStatus initializeStatus= initialize(arguments);
   		status.merge(initializeStatus);
    }

	public void setLinkedProposalModel(LinkedProposalModelCore linkedProposalModel) {
		fLinkedProposalModelCore= linkedProposalModel;
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

    @Override
	public String getName() {
        return RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_name;
    }

    private boolean useThisForFieldAccess() {
    	return StubUtility.useThisForFieldAccess(fCu.getJavaProject());
    }

    private boolean doAddComments() {
    	return StubUtility.doAddComments(fCu.getJavaProject());
    }

    @Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        RefactoringStatus result= Checks.validateModifiesFiles(
        	ResourceUtil.getFiles(new ICompilationUnit[]{fCu}),
			getValidationContext(), pm);
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
    	if (fCompilationUnitNode == null) {
    		fCompilationUnitNode= RefactoringASTParser.parseWithASTProvider(fCu, true, pm);
    	}
    	if (fAnonymousInnerClassNode == null) {
    		fAnonymousInnerClassNode= getAnonymousInnerClass(NodeFinder.perform(fCompilationUnitNode, fSelectionStart, fSelectionLength));
    	}
		if (fAnonymousInnerClassNode != null) {
			final AbstractTypeDeclaration declaration= ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
			if (declaration instanceof TypeDeclaration) {
				final AbstractTypeDeclaration[] nested= ((TypeDeclaration) declaration).getTypes();
				fClassNamesUsed= new HashSet<>(nested.length);
				for (AbstractTypeDeclaration element : nested)
					fClassNamesUsed.add(element.getName().getIdentifier());
			} else
				fClassNamesUsed= Collections.emptySet();
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
        return ASTNodes.getParent(node, AnonymousClassDeclaration.class);
    }

    public RefactoringStatus validateInput() {
        RefactoringStatus result= Checks.checkTypeName(fClassName, fCu);
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
        List<IVariableBinding> anonymousInnerFieldTypes = getAllEnclosingAnonymousTypesField();
        List<IBinding> accessedField = getAllAccessedFields();
        final Iterator<IVariableBinding> it = anonymousInnerFieldTypes.iterator();
        while(it.hasNext()) {
            final IVariableBinding variableBinding = it.next();
            final Iterator<IBinding> it2 = accessedField.iterator();
            while (it2.hasNext()) {
                IVariableBinding variableBinding2 = (IVariableBinding) it2.next();
                if(Bindings.equals(variableBinding, variableBinding2)) {
                    return true;
                }
            }
        }
        return false;
    }

	private List<IBinding> getAllAccessedFields() {
		final List<IBinding> accessedFields= new ArrayList<>();

		ASTVisitor visitor= new ASTVisitor() {

			@Override
			public boolean visit(FieldAccess node) {
				final IVariableBinding binding= node.resolveFieldBinding();
				if (binding != null && !binding.isEnumConstant())
					accessedFields.add(binding);
				return super.visit(node);
			}

			@Override
			public boolean visit(QualifiedName node) {
				final IBinding binding= node.resolveBinding();
				if (binding instanceof IVariableBinding) {
					IVariableBinding variable= (IVariableBinding) binding;
					if (!variable.isEnumConstant() && variable.isField())
						accessedFields.add(binding);
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(SimpleName node) {
				final IBinding binding= node.resolveBinding();
				if (binding instanceof IVariableBinding) {
					IVariableBinding variable= (IVariableBinding) binding;
					if (!variable.isEnumConstant() && variable.isField())
						accessedFields.add(binding);
				}
				return super.visit(node);
			}

			@Override
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

    private List<IVariableBinding> getAllEnclosingAnonymousTypesField() {
		final List<IVariableBinding> ans= new ArrayList<>();
		final AbstractTypeDeclaration declaration= ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
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
        ITypeBinding type= ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class).resolveBinding();
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
    @Override
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

    public CompilationUnitChange createCompilationUnitChange(IProgressMonitor pm) throws CoreException {
		final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fCu, fCompilationUnitNode);
		final ITypeBinding[] typeParameters= getTypeParameters();
		final ITypeBinding[] impliedSuperParameters= getImpliedClassCreationTypeParameters();
		addNestedClass(rewrite, typeParameters, impliedSuperParameters);
		modifyConstructorCall(rewrite, typeParameters);
		return rewrite.createChange(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_name, false, pm);
    }


    /*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		final CompilationUnitChange result= createCompilationUnitChange(pm);
		result.setDescriptor(createRefactoringDescriptor());
		return result;
	}

	private ITypeBinding[] getImpliedClassCreationTypeParameters() {
		final ClassInstanceCreation creation= (ClassInstanceCreation) fAnonymousInnerClassNode.getParent();
		// In assignments and variable declaration statements, a type parameter might exist in the
		// left side of the statement and implied in the class creation (e.g. Set<String> x= new HashSet<>();)
		ASTNode nodeOfInterest= ASTNodes.getFirstAncestorOrNull(creation, Assignment.class, FieldDeclaration.class, VariableDeclarationStatement.class);
		if (nodeOfInterest instanceof Assignment) {
			Assignment assignment= (Assignment)nodeOfInterest;
			ITypeBinding typeBinding= assignment.resolveTypeBinding();
			if (typeBinding != null && typeBinding.isParameterizedType()) {
				return typeBinding.getTypeArguments();
			}
		} else if (nodeOfInterest instanceof FieldDeclaration) {
			FieldDeclaration fieldDeclaration= (FieldDeclaration)nodeOfInterest;
			Type fieldType= fieldDeclaration.getType();
			if (fieldType.isParameterizedType()) {
				ITypeBinding typeBinding= fieldType.resolveBinding();
				if (typeBinding != null) {
					return typeBinding.getTypeArguments();
				}
			}
		} else if (nodeOfInterest instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement vds= (VariableDeclarationStatement)nodeOfInterest;
			Type vdsType= vds.getType();
			if (vdsType.isParameterizedType()) {
				ITypeBinding typeBinding= vdsType.resolveBinding();
				if (typeBinding != null) {
					return typeBinding.getTypeArguments();
				}
			}
		}
		return new ITypeBinding[0];
	}

	private ITypeBinding[] getTypeParameters() {
		final List<ITypeBinding> parameters= new ArrayList<>(4);
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
				for (final Iterator<TypeParameter> iterator= declaration.typeParameters().iterator(); iterator.hasNext();) {
					parameter= iterator.next();
					binding= parameter.resolveBinding();
					if (binding != null)
						parameters.add(binding);
				}
			}
		}
		final TypeVariableFinder finder= new TypeVariableFinder();
		creation.accept(finder);
		final ITypeBinding[] variables= finder.getResult();
		final List<ITypeBinding> remove= new ArrayList<>(4);
		boolean match= false;
		ITypeBinding binding= null;
		ITypeBinding variable= null;
		for (final Iterator<ITypeBinding> iterator= parameters.iterator(); iterator.hasNext();) {
			match= false;
			binding= iterator.next();
			for (ITypeBinding variable2 : variables) {
				variable= variable2;
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

	private RefactoringChangeDescriptor createRefactoringDescriptor() {
		final ITypeBinding binding= fAnonymousInnerClassNode.resolveBinding();
		final String[] labels= new String[] { BindingLabelProviderCore.getBindingLabel(binding, JavaElementLabelsCore.ALL_FULLY_QUALIFIED), BindingLabelProviderCore.getBindingLabel(binding.getDeclaringMethod(), JavaElementLabelsCore.ALL_FULLY_QUALIFIED)};
		final Map<String, String> arguments= new HashMap<>();
		final String projectName= fCu.getJavaProject().getElementName();
		final int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | JavaRefactoringDescriptor.JAR_REFACTORING | JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		final String description= RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_descriptor_description_short;
		final String header= Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_descriptor_description, labels);
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(projectName, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_original_pattern, BindingLabelProviderCore.getBindingLabel(binding, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_class_name_pattern, BasicElementLabels.getJavaElementName(fClassName)));
		String visibility= JdtFlags.getVisibilityString(fVisibility);
		if (visibility.length() == 0)
			visibility= RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_default_visibility;
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_visibility_pattern, visibility));
		if (fDeclareFinal && fDeclareStatic)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_final_static);
		else if (fDeclareFinal)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_final);
		else if (fDeclareStatic)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_static);
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, JavaRefactoringDescriptorUtil.elementToHandle(projectName, fCu));
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME, fClassName);
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION, Integer.toString(fSelectionStart) + ' ' + Integer.toString(fSelectionLength));
		arguments.put(ATTRIBUTE_FINAL, Boolean.toString(fDeclareFinal));
		arguments.put(ATTRIBUTE_STATIC, Boolean.toString(fDeclareStatic));
		arguments.put(ATTRIBUTE_VISIBILITY, Integer.toString(fVisibility));

		ConvertAnonymousDescriptor descriptor= RefactoringSignatureDescriptorFactory.createConvertAnonymousDescriptor(projectName, description, comment.asString(), arguments, flags);
		return new RefactoringChangeDescriptor(descriptor);
	}

	private void modifyConstructorCall(CompilationUnitRewrite rewrite, ITypeBinding[] parameters) {
		rewrite.getASTRewrite().replace(fAnonymousInnerClassNode.getParent(), createNewClassInstanceCreation(rewrite, parameters), null);
	}

	private ASTNode createNewClassInstanceCreation(CompilationUnitRewrite rewrite, ITypeBinding[] parameters) {
		AST ast= fAnonymousInnerClassNode.getAST();
		ClassInstanceCreation newClassCreation= ast.newClassInstanceCreation();
		newClassCreation.setAnonymousClassDeclaration(null);
		Type type= null;
		SimpleName newNameNode= ast.newSimpleName(fClassName);
		if (parameters.length > 0) {
			final ParameterizedType parameterized= ast.newParameterizedType(ast.newSimpleType(newNameNode));
			for (ITypeBinding parameter : parameters)
				parameterized.typeArguments().add(ast.newSimpleType(ast.newSimpleName(parameter.getName())));
			type= parameterized;
		} else
			type= ast.newSimpleType(newNameNode);
		newClassCreation.setType(type);
		copyArguments(rewrite, newClassCreation);
		addArgumentsForLocalsUsedInInnerClass(newClassCreation);

		addLinkedPosition(KEY_TYPE_NAME, newNameNode, rewrite.getASTRewrite(), true);

		ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation) fAnonymousInnerClassNode.getParent();
		if (classInstanceCreation.getExpression() instanceof SimpleName) {
			String name= ((SimpleName) classInstanceCreation.getExpression()).getIdentifier();
			if (name != null && !name.isEmpty()) {
				newClassCreation.setExpression(ast.newSimpleName(name));
			}
		}
		return newClassCreation;
	}

	private void addArgumentsForLocalsUsedInInnerClass(ClassInstanceCreation newClassCreation) {
        for (final IVariableBinding binding : getUsedLocalVariables()) {
            final AST ast= fAnonymousInnerClassNode.getAST();
			Name name= null;
			if (binding.isEnumConstant())
				name= ast.newQualifiedName(ast.newSimpleName(binding.getDeclaringClass().getName()), ast.newSimpleName(binding.getName()));
			else
				name= ast.newSimpleName(binding.getName());
			newClassCreation.arguments().add(name);
        }
    }

    private void copyArguments(CompilationUnitRewrite rewrite, ClassInstanceCreation newClassCreation) {
    	Iterator<Expression> iter= ((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).arguments().iterator();
    	if (!iter.hasNext())
    		return;

    	IMethodBinding superConstructorBinding= getSuperConstructorBinding();
    	ITypeBinding[] parameterTypes= superConstructorBinding.getParameterTypes();

    	List<Expression> arguments= newClassCreation.arguments();
		ASTRewrite astRewrite= rewrite.getASTRewrite();
		int last= parameterTypes.length - 1;

		for (int i= 0; i < last; i++) {
			arguments.add((Expression) astRewrite.createCopyTarget(iter.next()));
		}
    	if (superConstructorBinding.isVarargs()) {
    		AST ast= astRewrite.getAST();
    		ArrayCreation arrayCreation= ast.newArrayCreation();
    		arrayCreation.setType((ArrayType) rewrite.getImportRewrite().addImport(parameterTypes[last], ast));
    		ArrayInitializer initializer= ast.newArrayInitializer();
    		arrayCreation.setInitializer(initializer);
    		arguments.add(arrayCreation);
    		arguments= initializer.expressions();
    	}
    	while (iter.hasNext()) {
    		arguments.add((Expression) astRewrite.createCopyTarget(iter.next()));
    	}
    }

    private void addNestedClass(CompilationUnitRewrite rewrite, ITypeBinding[] typeParameters, ITypeBinding[] impliedSuperTypeParameters) throws CoreException {
        final AbstractTypeDeclaration declarations= ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
        int index= findIndexOfFistNestedClass(declarations.bodyDeclarations());
        if (index == -1)
            index= 0;
        rewrite.getASTRewrite().getListRewrite(declarations, declarations.getBodyDeclarationsProperty()).insertAt(createNewNestedClass(rewrite, typeParameters, impliedSuperTypeParameters), index, null);
    }

    private static int findIndexOfFistNestedClass(List<BodyDeclaration> list) {
        for (int i= 0, n= list.size(); i < n; i++) {
            BodyDeclaration each= list.get(i);
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

    private AbstractTypeDeclaration createNewNestedClass(CompilationUnitRewrite rewrite, ITypeBinding[] typeParameters, ITypeBinding[] impliedSuperTypeParameters) throws CoreException {
		final AST ast= fAnonymousInnerClassNode.getAST();

		final TypeDeclaration newDeclaration= ast.newTypeDeclaration();
		newDeclaration.setInterface(false);
		newDeclaration.setJavadoc(null);
		newDeclaration.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiersForNestedClass()));
		newDeclaration.setName(ast.newSimpleName(fClassName));

		TypeParameter parameter= null;
		for (ITypeBinding typeParameter : typeParameters) {
			parameter= ast.newTypeParameter();
			parameter.setName(ast.newSimpleName(typeParameter.getName()));
			newDeclaration.typeParameters().add(parameter);
		}
		setSuperType(rewrite, newDeclaration, impliedSuperTypeParameters);

		IJavaProject project= fCu.getJavaProject();

		IVariableBinding[] bindings= getUsedLocalVariables();
		ArrayList<String> fieldNames= new ArrayList<>();
		for (int i= 0; i < bindings.length; i++) {
			String name= StubUtility.getBaseName(bindings[i], project);
			String[] fieldNameProposals= StubUtility.getVariableNameSuggestions(NamingConventions.VK_INSTANCE_FIELD, project, name, 0, fieldNames, true);
			fieldNames.add(fieldNameProposals[0]);


			if (fLinkedProposalModelCore != null) {
				LinkedProposalPositionGroupCore positionGroup= fLinkedProposalModelCore.getPositionGroup(KEY_FIELD_NAME_EXT + i, true);
				for (int k= 0; k < fieldNameProposals.length; k++) {
					positionGroup.addProposal(fieldNameProposals[k], fieldNameProposals.length - k);
				}
			}
		}
		String[] allFieldNames= fieldNames.toArray(new String[fieldNames.size()]);

		List<BodyDeclaration> newBodyDeclarations= newDeclaration.bodyDeclarations();

		createFieldsForAccessedLocals(rewrite, bindings, allFieldNames, newBodyDeclarations);

		MethodDeclaration newConstructorDecl= createNewConstructor(rewrite, bindings, allFieldNames);
		if (newConstructorDecl != null) {
			newBodyDeclarations.add(newConstructorDecl);
		}

		updateAndMoveBodyDeclarations(rewrite, bindings, allFieldNames, newBodyDeclarations, newConstructorDecl);

		if (doAddComments()) {
			String[] parameterNames= new String[typeParameters.length];
			for (int index= 0; index < parameterNames.length; index++) {
				parameterNames[index]= typeParameters[index].getName();
			}
			String string= CodeGeneration.getTypeComment(rewrite.getCu(), fClassName, parameterNames, StubUtility.getLineDelimiterUsed(fCu));
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.getASTRewrite().createStringPlaceholder(string, ASTNode.JAVADOC);
				newDeclaration.setJavadoc(javadoc);
			}
		}
		if (fLinkedProposalModelCore != null) {
			addLinkedPosition(KEY_TYPE_NAME, newDeclaration.getName(), rewrite.getASTRewrite(), false);
			ModifierCorrectionSubProcessorCore.installLinkedVisibilityProposals(fLinkedProposalModelCore, rewrite.getASTRewrite(), newDeclaration.modifiers(), false);
		}

		return newDeclaration;
	}

	private void updateAndMoveBodyDeclarations(CompilationUnitRewrite rewriter, IVariableBinding[] bindings, String[] fieldNames, List<BodyDeclaration> newBodyDeclarations, MethodDeclaration newConstructorDecl) {
		final ASTRewrite astRewrite= rewriter.getASTRewrite();
		final AST ast= astRewrite.getAST();

		final boolean useThisAccess= useThisForFieldAccess();

		int fieldInsertIndex= newConstructorDecl != null ? newBodyDeclarations.lastIndexOf(newConstructorDecl) : newBodyDeclarations.size();

		for (Iterator<BodyDeclaration> iterator= fAnonymousInnerClassNode.bodyDeclarations().iterator(); iterator.hasNext();) {
			BodyDeclaration body= iterator.next();

			for (int i= 0; i < bindings.length; i++) {
				String fieldName= fieldNames[i];
				for (SimpleName name : LinkedNodeFinder.findByBinding(body, bindings[i])) {
					SimpleName newNode= ast.newSimpleName(fieldName);
					if (useThisAccess) {
						FieldAccess access= ast.newFieldAccess();
						access.setExpression(ast.newThisExpression());
						access.setName(newNode);
						astRewrite.replace(name, access, null);
					} else {
						astRewrite.replace(name, newNode, null);
					}
					addLinkedPosition(KEY_FIELD_NAME_EXT + i, newNode, astRewrite, false);
				}
			}
			if (body instanceof Initializer || body instanceof FieldDeclaration) {
				newBodyDeclarations.add(fieldInsertIndex++, (BodyDeclaration) astRewrite.createMoveTarget(body));
			} else {
				newBodyDeclarations.add((BodyDeclaration) astRewrite.createMoveTarget(body));
			}
		}

		if (newConstructorDecl != null) {
			// move initialization of existing fields to constructor if an outer is referenced
			List<Statement> bodyStatements= newConstructorDecl.getBody().statements();

			for (VariableDeclarationFragment fragment : getFieldsToInitializeInConstructor()) {
				Expression initializer= fragment.getInitializer();
				Expression replacement= (Expression) astRewrite.get(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY);
				if (replacement == initializer) {
					replacement= (Expression) astRewrite.createMoveTarget(initializer);
				}
				astRewrite.remove(initializer, null);
				SimpleName fieldNameNode= ast.newSimpleName(fragment.getName().getIdentifier());
				bodyStatements.add(newFieldAssignment(ast, fieldNameNode, replacement, useThisAccess));
			}
		}
	}

    private void createFieldsForAccessedLocals(CompilationUnitRewrite rewrite, IVariableBinding[] varBindings, String[] fieldNames, List<BodyDeclaration> newBodyDeclarations) throws CoreException {
		final ImportRewrite importRewrite= rewrite.getImportRewrite();
		final ASTRewrite astRewrite= rewrite.getASTRewrite();
		final AST ast= astRewrite.getAST();

		for (int i= 0; i < varBindings.length; i++) {
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setInitializer(null);
			fragment.setName(ast.newSimpleName(fieldNames[i]));
			FieldDeclaration field= ast.newFieldDeclaration(fragment);
			ITypeBinding varType= varBindings[i].getType();
			field.setType(importRewrite.addImport(varType, ast));
			field.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.PRIVATE | Modifier.FINAL));
			if (doAddComments()) {
				String string= CodeGeneration.getFieldComment(rewrite.getCu(), varType.getName(), fieldNames[i], StubUtility.getLineDelimiterUsed(fCu));
				if (string != null) {
					Javadoc javadoc= (Javadoc) astRewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
					field.setJavadoc(javadoc);
				}
			}

			newBodyDeclarations.add(field);

			addLinkedPosition(KEY_FIELD_NAME_EXT + i, fragment.getName(), astRewrite, false);
		}
	}

    private void addLinkedPosition(String key, ASTNode nodeToTrack, ASTRewrite rewrite, boolean isFirst) {
    	if (fLinkedProposalModelCore != null) {
    		fLinkedProposalModelCore.getPositionGroup(key, true).addPosition(rewrite.track(nodeToTrack), isFirst);
    	}
    }


    private IVariableBinding[] getUsedLocalVariables() {
        final Set<IBinding> result= new HashSet<>(0);
        collectRefrencedVariables(fAnonymousInnerClassNode, result);
        ArrayList<IVariableBinding> usedLocals= new ArrayList<>();
        for (IBinding iBinding : result) {
        	IVariableBinding next= (IVariableBinding) iBinding;
			if (isBindingToTemp(next)) {
        		usedLocals.add(next);
        	}
		}
        return usedLocals.toArray(new IVariableBinding[usedLocals.size()]);
    }

    private void collectRefrencedVariables(ASTNode root, final Set<IBinding> result) {
    	root.accept(new ASTVisitor() {
            @Override
			public boolean visit(SimpleName node) {
                IBinding binding= node.resolveBinding();
                if (binding instanceof IVariableBinding)
                	result.add(binding);
                return true;
            }
        });
    }

    private boolean isBindingToTemp(IVariableBinding variable) {
		if (variable.isField())
			return false;
		if (!Modifier.isFinal(variable.getModifiers()) && !JavaModelUtil.is1d8OrHigher(fCu.getJavaProject()))
			return false;
		ASTNode declaringNode= fCompilationUnitNode.findDeclaringNode(variable);
		if (declaringNode == null)
			return false;
		if (ASTNodes.isParent(declaringNode, fAnonymousInnerClassNode))
			return false;
		return true;
	}

    private MethodDeclaration createNewConstructor(CompilationUnitRewrite rewrite, IVariableBinding[] bindings, String[] fieldNames) throws JavaModelException {
    	ClassInstanceCreation instanceCreation= (ClassInstanceCreation) fAnonymousInnerClassNode.getParent();

    	if (instanceCreation.arguments().isEmpty() && bindings.length == 0)
			return null;

    	IJavaProject project= fCu.getJavaProject();
        AST ast= rewrite.getAST();
        ImportRewrite importRewrite= rewrite.getImportRewrite();
        ASTRewrite astRewrite= rewrite.getASTRewrite();

		MethodDeclaration newConstructor= ast.newMethodDeclaration();
		newConstructor.setConstructor(true);
		newConstructor.setJavadoc(null);
		newConstructor.modifiers().addAll(ASTNodeFactory.newModifiers(ast, fVisibility));
		newConstructor.setName(ast.newSimpleName(fClassName));
		addLinkedPosition(KEY_TYPE_NAME, newConstructor.getName(), astRewrite, false);

		newConstructor.setBody(ast.newBlock());

		List<Statement> newStatements= newConstructor.getBody().statements();

        List<SingleVariableDeclaration> newParameters= newConstructor.parameters();
        List<String> newParameterNames= new ArrayList<>();

        // add parameters for elements passed with the instance creation
        if (!instanceCreation.arguments().isEmpty()) {
        	IMethodBinding constructorBinding= getSuperConstructorBinding();
        	if (constructorBinding != null) {
        		SuperConstructorInvocation superConstructorInvocation= ast.newSuperConstructorInvocation();
    			ITypeBinding[] parameterTypes= constructorBinding.getParameterTypes();
    	        String[][] parameterNames= StubUtility.suggestArgumentNamesWithProposals(project, constructorBinding);
    	        for (int i= 0; i < parameterNames.length; i++) {
    	        	String[] nameProposals= parameterNames[i];
    	        	String paramName= nameProposals[0];

    	    		SingleVariableDeclaration param= newParameterDeclaration(ast, importRewrite, paramName, parameterTypes[i]);
    	    		newParameters.add(param);
    	    		newParameterNames.add(paramName);

    	    		SimpleName newSIArgument= ast.newSimpleName(paramName);
					superConstructorInvocation.arguments().add(newSIArgument);

					if (fLinkedProposalModelCore != null) {
						LinkedProposalPositionGroupCore positionGroup= fLinkedProposalModelCore.getPositionGroup(KEY_PARAM_NAME_CONST+ String.valueOf(i), true);
						positionGroup.addPosition(astRewrite.track(param.getName()), false);
						positionGroup.addPosition(astRewrite.track(newSIArgument), false);
	        	        for (int k= 0; k < nameProposals.length; k++) {
	        	        	positionGroup.addProposal(nameProposals[k], nameProposals.length - k);
						}
					}
    	        }
    	        newStatements.add(superConstructorInvocation);
        	}
        }
        // add parameters for all outer variables used
        boolean useThisAccess= useThisForFieldAccess();
        for (int i= 0; i < bindings.length; i++) {
        	String baseName= StubUtility.getBaseName(bindings[i], project);
        	String[] paramNameProposals= StubUtility.getVariableNameSuggestions(NamingConventions.VK_PARAMETER, project, baseName, 0, newParameterNames, true);
        	String paramName= paramNameProposals[0];

        	SingleVariableDeclaration param= newParameterDeclaration(ast, importRewrite, paramName, bindings[i].getType());
    		newParameters.add(param);
    		newParameterNames.add(paramName);

    		String fieldName= fieldNames[i];
    		SimpleName fieldNameNode= ast.newSimpleName(fieldName);
    		SimpleName paramNameNode= ast.newSimpleName(paramName);
			newStatements.add(newFieldAssignment(ast, fieldNameNode, paramNameNode, useThisAccess || newParameterNames.contains(fieldName)));

			if (fLinkedProposalModelCore != null) {
				LinkedProposalPositionGroupCore positionGroup= fLinkedProposalModelCore.getPositionGroup(KEY_PARAM_NAME_EXT+ String.valueOf(i), true);
				positionGroup.addPosition(astRewrite.track(param.getName()), false);
				positionGroup.addPosition(astRewrite.track(paramNameNode), false);
    	        for (int k= 0; k < paramNameProposals.length; k++) {
    	        	positionGroup.addProposal(paramNameProposals[k], paramNameProposals.length - k);
				}

    	        fLinkedProposalModelCore.getPositionGroup(KEY_FIELD_NAME_EXT + i, true).addPosition(astRewrite.track(fieldNameNode), false);
			}
		}

		addExceptionsToNewConstructor(newConstructor, importRewrite);

		if (doAddComments()) {
			try {
				String[] allParamNames= newParameterNames.toArray(new String[newParameterNames.size()]);
				String string= CodeGeneration.getMethodComment(fCu, fClassName, fClassName, allParamNames, new String[0], null, new String[0], null, StubUtility.getLineDelimiterUsed(fCu));
				if (string != null) {
					Javadoc javadoc= (Javadoc) astRewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
					newConstructor.setJavadoc(javadoc);
				}
			} catch (CoreException exception) {
				throw new JavaModelException(exception);
			}
		}
		return newConstructor;
	}

    private Statement newFieldAssignment(AST ast, SimpleName fieldNameNode, Expression initializer, boolean useThisAccess) {
		Assignment assignment= ast.newAssignment();
		if (useThisAccess) {
			FieldAccess access= ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(fieldNameNode);
			assignment.setLeftHandSide(access);
		} else {
			assignment.setLeftHandSide(fieldNameNode);
		}
		assignment.setOperator(Assignment.Operator.ASSIGN);
		assignment.setRightHandSide(initializer);

		return ast.newExpressionStatement(assignment);
     }


    // live List of VariableDeclarationFragments
    private List<VariableDeclarationFragment> getFieldsToInitializeInConstructor() {
        List<VariableDeclarationFragment> result= new ArrayList<>(0);
        for (Iterator<BodyDeclaration> iter= fAnonymousInnerClassNode.bodyDeclarations().iterator(); iter.hasNext(); ) {
            Object element= iter.next();
            if (element instanceof FieldDeclaration) {
            	List<VariableDeclarationFragment> fragments= ((FieldDeclaration) element).fragments();
                for (VariableDeclarationFragment fragment : fragments) {
                    if (isToBeInitializerInConstructor(fragment, result))
                        result.add(fragment);
                }
            }
        }
        return result;
    }

    private boolean isToBeInitializerInConstructor(VariableDeclarationFragment fragment, List<VariableDeclarationFragment> fieldsToInitialize) {
    	return fragment.getInitializer() != null && areLocalsUsedIn(fragment.getInitializer(), fieldsToInitialize);
    }

    private boolean areLocalsUsedIn(Expression fieldInitializer, List<VariableDeclarationFragment> fieldsToInitialize) {
        Set<IBinding> localsUsed= new HashSet<>(0);
        collectRefrencedVariables(fieldInitializer, localsUsed);

        ITypeBinding anonType= fAnonymousInnerClassNode.resolveBinding();

        for (IBinding iBinding : localsUsed) {
			IVariableBinding curr= (IVariableBinding) iBinding;
			if (isBindingToTemp(curr) // Reference a local from outside
					|| (curr.isField() && curr.getDeclaringClass() == anonType && fieldsToInitialize.contains(fCompilationUnitNode.findDeclaringNode(curr)))) { // References a field that references a local from outside
				return true;
			}
		}
        return false;
    }

    private IMethodBinding getSuperConstructorBinding() {
        //workaround for missing java core functionality - finding a
        // super constructor for an anonymous class creation
        IMethodBinding anonConstr= ((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).resolveConstructorBinding();
        if (anonConstr == null)
            return null;
        ITypeBinding superClass= anonConstr.getDeclaringClass().getSuperclass();
        for (IMethodBinding superMethod : superClass.getDeclaredMethods()) {
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

	private void addExceptionsToNewConstructor(MethodDeclaration newConstructor, ImportRewrite importRewrite) {
        IMethodBinding constructorBinding= getSuperConstructorBinding();
        if (constructorBinding == null)
            return;
        for (ITypeBinding exception : constructorBinding.getExceptionTypes()) {
			Type exceptionType= importRewrite.addImport(exception, fAnonymousInnerClassNode.getAST());
			newConstructor.thrownExceptionTypes().add(exceptionType);
        }
    }

    private SingleVariableDeclaration newParameterDeclaration(AST ast, ImportRewrite importRewrite, String paramName, ITypeBinding paramType) {
    	SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
		param.setType(importRewrite.addImport(paramType, ast));
		param.setName(ast.newSimpleName(paramName));
		return param;
    }

    private void setSuperType(CompilationUnitRewrite rewrite, TypeDeclaration declaration, ITypeBinding[] impliedSuperTypeParameters) {
		ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation) fAnonymousInnerClassNode.getParent();
		ITypeBinding binding= classInstanceCreation.resolveTypeBinding();
		if (binding == null)
			return;
		Type newType= null;
		if (impliedSuperTypeParameters.length == 0 || !classInstanceCreation.getType().isParameterizedType()) {
			newType= (Type) ASTNode.copySubtree(fAnonymousInnerClassNode.getAST(), classInstanceCreation.getType());
		} else {
			ImportRewrite importRewriter= rewrite.getImportRewrite();
			AST ast= fAnonymousInnerClassNode.getAST();
			Type oldType= classInstanceCreation.getType();
			Type newBaseType= (Type) ASTNode.copySubtree(ast, ((ParameterizedType) oldType).getType());
			newType= ast.newParameterizedType(newBaseType);
			for (ITypeBinding impliedTypeBinding : impliedSuperTypeParameters) {
				Type type= importRewriter.addImport(impliedTypeBinding, ast);
				((ParameterizedType) newType).typeArguments().add(type);
			}
		}
		if ("java.lang.Object".equals(binding.getSuperclass().getQualifiedName())) { //$NON-NLS-1$
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
        ITypeBinding typeBinding = ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class).resolveBinding();
        ASTNode current = fAnonymousInnerClassNode.getParent();
        boolean ans = false;
        while(current != null) {
            switch(current.getNodeType()) {
				case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				case ASTNode.CONSTRUCTOR_INVOCATION:
					return true;
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
                case ASTNode.CLASS_INSTANCE_CREATION:
                {
                	if(((ClassInstanceCreation)current).getExpression() != null) {
                		// class creation is part of an expression, then we don't want static
                		return false;
                	}
                }
            }
            current = current.getParent();
        }
        return ans;
    }

    private RefactoringStatus initialize(JavaRefactoringArguments arguments) {
		fSelfInitializing= true;
		final String handle= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(arguments.getProject(), handle, false);
			if (element == null || !element.exists() || element.getElementType() != IJavaElement.COMPILATION_UNIT)
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getName(), IJavaRefactorings.CONVERT_ANONYMOUS);
			else {
				fCu= (ICompilationUnit) element;
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
		final String name= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME);
		if (name != null && !"".equals(name)) //$NON-NLS-1$
			fClassName= name;
		else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME));
		final String visibility= arguments.getAttribute(ATTRIBUTE_VISIBILITY);
		if (visibility != null && !"".equals(visibility)) {//$NON-NLS-1$
			int flag= 0;
			try {
				flag= Integer.parseInt(visibility);
			} catch (NumberFormatException exception) {
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_VISIBILITY));
			}
			fVisibility= flag;
		}
		final String selection= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION);
		if (selection != null) {
			int offset= -1;
			int length= -1;
			final StringTokenizer tokenizer= new StringTokenizer(selection);
			if (tokenizer.hasMoreTokens())
				offset= Integer.parseInt(tokenizer.nextToken());
			if (tokenizer.hasMoreTokens())
				length= Integer.parseInt(tokenizer.nextToken());
			if (offset >= 0 && length >= 0) {
				fSelectionStart= offset;
				fSelectionLength= length;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection, JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION}));
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION));
		final String declareStatic= arguments.getAttribute(ATTRIBUTE_STATIC);
		if (declareStatic != null) {
			fDeclareStatic= Boolean.parseBoolean(declareStatic);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STATIC));
		final String declareFinal= arguments.getAttribute(ATTRIBUTE_FINAL);
		if (declareFinal != null) {
			fDeclareFinal= Boolean.parseBoolean(declareFinal);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FINAL));
		return new RefactoringStatus();
	}
}
