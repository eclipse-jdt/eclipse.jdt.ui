/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.SourceRangeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTesterCore;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameAnalyzeUtil.LocalAnalyzePackage;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.refactoring.IRefactoringSaveModes;

public class RenameLocalVariableProcessor extends JavaRenameProcessor implements IReferenceUpdating {

	private ILocalVariable fLocalVariable;
	private ICompilationUnit fCu;

	//the following fields are set or modified after the construction
	private boolean fUpdateReferences;
	private String fCurrentName;
	private String fNewName;
	private CompilationUnit fCompilationUnitNode;
	private VariableDeclaration fTempDeclarationNode;
	private CompilationUnitChange fChange;

	private boolean fIsComposite;
	private GroupCategorySet fCategorySet;
	private TextChangeManager fChangeManager;
	private RenameAnalyzeUtil.LocalAnalyzePackage fLocalAnalyzePackage;

	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameLocalVariableProcessor"; //$NON-NLS-1$

	/**
	 * Creates a new rename local variable processor.
	 * @param localVariable the local variable, or <code>null</code> if invoked by scripting
	 */
	public RenameLocalVariableProcessor(ILocalVariable localVariable) {
		fLocalVariable= localVariable;
		fUpdateReferences= true;
		if (localVariable != null)
			fCu= (ICompilationUnit) localVariable.getAncestor(IJavaElement.COMPILATION_UNIT);
		fNewName= ""; //$NON-NLS-1$
		fIsComposite= false;
	}

	public RenameLocalVariableProcessor(JavaRefactoringArguments arguments, RefactoringStatus status) {
		this(null);
		RefactoringStatus initializeStatus= initialize(arguments);
		status.merge(initializeStatus);
	}

	/**
	 * Creates a new rename local variable processor.
	 * <p>
	 * This constructor is only used by <code>RenameTypeProcessor</code>.
	 * </p>
	 *
	 * @param localVariable the local variable
	 * @param manager the change manager
	 * @param node the compilation unit node
	 * @param categorySet the group category set
	 */
	public RenameLocalVariableProcessor(ILocalVariable localVariable, TextChangeManager manager, CompilationUnit node, GroupCategorySet categorySet) {
		this(localVariable);
		fChangeManager= manager;
		fCategorySet= categorySet;
		fCompilationUnitNode= node;
		fIsComposite= true;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#getAffectedProjectNatures()
	 */
	@Override
	protected final String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fLocalVariable);
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	@Override
	public Object[] getElements() {
		return new Object[] { fLocalVariable };
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	@Override
	public String getProcessorName() {
		return RefactoringCoreMessages.RenameTempRefactoring_rename;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	@Override
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTesterCore.isRenameAvailable(fLocalVariable);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#getUpdateReferences()
	 */
	@Override
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating#setUpdateReferences(boolean)
	 */
	@Override
	public void setUpdateReferences(boolean updateReferences) {
		fUpdateReferences= updateReferences;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#getCurrentElementName()
	 */
	@Override
	public String getCurrentElementName() {
		return fCurrentName;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#getNewElementName()
	 */
	@Override
	public String getNewElementName() {
		return fNewName;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#setNewElementName(java.lang.String)
	 */
	@Override
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#getNewElement()
	 */
	@Override
	public Object getNewElement() {
		return null; //cannot create an ILocalVariable
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		initAST();
		if (fTempDeclarationNode == null || fTempDeclarationNode.resolveBinding() == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_must_select_local);

		if (!Checks.isDeclaredIn(fTempDeclarationNode, MethodDeclaration.class)
				&& !Checks.isDeclaredIn(fTempDeclarationNode, Initializer.class)
				&& !Checks.isDeclaredIn(fTempDeclarationNode, LambdaExpression.class)
				&& !Checks.isDeclaredIn(fTempDeclarationNode, RecordDeclaration.class)) {
			if (JavaModelUtil.is1d8OrHigher(fCu.getJavaProject()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_only_in_methods_initializers_and_lambda);

			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_only_in_methods_and_initializers);
		}

		initNames();
		return new RefactoringStatus();
	}

	private void initAST() {
		if (!fIsComposite)
			fCompilationUnitNode= RefactoringASTParser.parseWithASTProvider(fCu, true, null);
		ISourceRange sourceRange= fLocalVariable.getNameRange();
		ASTNode name= NodeFinder.perform(fCompilationUnitNode, sourceRange);
		if (name == null)
			return;
		if (name.getParent() instanceof VariableDeclaration)
			fTempDeclarationNode= (VariableDeclaration) name.getParent();
	}

	private void initNames(){
		fCurrentName= fTempDeclarationNode.getName().getIdentifier();
	}

	@Override
	protected RenameModifications computeRenameModifications() throws CoreException {
		RenameModifications result= new RenameModifications();
		result.rename(fLocalVariable, new RenameArguments(getNewElementName(), getUpdateReferences()));
		return result;
	}

	@Override
	protected IFile[] getChangedFiles() throws CoreException {
		return new IFile[] {ResourceUtil.getFile(fCu)};
	}

	@Override
	public int getSaveMode() {
		return IRefactoringSaveModes.SAVE_NOTHING;
	}

	@Override
	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		try {
			pm.beginTask("", 1);	 //$NON-NLS-1$

			RefactoringStatus result= checkNewElementName(fNewName);
			if (result.hasFatalError())
				return result;
			result.merge(checkFieldCollision());
			if (result.hasFatalError()) {
				return result;
			}
			createEdits();
			if (!fIsComposite) {
				LocalAnalyzePackage[] localAnalyzePackages= new RenameAnalyzeUtil.LocalAnalyzePackage[] { fLocalAnalyzePackage };
				result.merge(RenameAnalyzeUtil.analyzeLocalRenames(localAnalyzePackages, fChange, fCompilationUnitNode, true));
			}
			return result;
		} finally {
			pm.done();
			if (fIsComposite) {
				// end of life cycle for this processor
				fChange= null;
				fCompilationUnitNode= null;
				fTempDeclarationNode= null;
			}
		}
	}

	private RefactoringStatus checkFieldCollision() {
		RefactoringStatus result= new RefactoringStatus();
		final IVariableBinding tempBinding= fTempDeclarationNode.resolveBinding();
		if (tempBinding != null) {
			ASTVisitor checkCollistionVisitor= new ASTVisitor() {
				@Override
				public boolean visit(SimpleName node) {
					if (node.getFullyQualifiedName().equals(tempBinding.getName()) && !(node.getParent() instanceof FieldAccess) &&
							!(node.getParent() instanceof QualifiedName) && !(node.getParent() instanceof VariableDeclaration)) {
						IBinding binding= node.resolveBinding();
						if (tempBinding.isEqualTo(binding)) {
							ASTNode ancestor= ASTNodes.getFirstAncestorOrNull(node, AbstractTypeDeclaration.class, AnonymousClassDeclaration.class);
							if (ancestor instanceof AbstractTypeDeclaration typeDecl) {
								ITypeBinding typeDeclBinding= typeDecl.resolveBinding();
								if (typeDeclBinding != null) {
									IVariableBinding[] fields= typeDeclBinding.getDeclaredFields();
									for (IVariableBinding field : fields) {
										if (field.getName().equals(fNewName)) {
											try {
												result.merge(RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.RenameTempRefactoring_field_collision, typeDeclBinding.getName() + "." + field.getName()), //$NON-NLS-1$
														new JavaStringStatusContext(fCu.getSource(),
																SourceRangeFactory.create(node))));
											} catch (JavaModelException e) {
												// should not happen
											}
											throw new AbortSearchException();
										}
									}
								}
							} else if (ancestor instanceof AnonymousClassDeclaration anonDecl) {
								ITypeBinding anonDeclBinding= anonDecl.resolveBinding();
								if (anonDeclBinding != null) {
									IVariableBinding[] fields= anonDeclBinding.getDeclaredFields();
									for (IVariableBinding field : fields) {
										if (field.getName().equals(fNewName)) {
											try {
												result.merge(RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.RenameTempRefactoring_field_collision, "ANONYMOUS." + field.getName()), //$NON-NLS-1$
														new JavaStringStatusContext(fCu.getSource(),
																SourceRangeFactory.create(node))));
											} catch (JavaModelException e) {
												// should not happen
											}
											result.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.RenameTempRefactoring_field_collision, "ANONYMOUS." + field.getName()))); //$NON-NLS-1$
											throw new AbortSearchException();
										}
									}
								}
							}
						}
					}
					return false;
				}
			};
			try {
				fCompilationUnitNode.accept(checkCollistionVisitor);
			} catch (AbortSearchException e) {
				// do nothing
			}
		}
		return result;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#checkNewElementName(java.lang.String)
	 */
	@Override
	public RefactoringStatus checkNewElementName(String newName) throws JavaModelException {
		RefactoringStatus result= Checks.checkFieldName(newName, fCu);
		if (! Checks.startsWithLowerCase(newName))
			if (fIsComposite) {
				final String nameOfParent= JavaElementLabelsCore.getElementLabel(fLocalVariable.getParent(), JavaElementLabelsCore.ALL_DEFAULT);
				final String nameOfType= JavaElementLabelsCore.getElementLabel(fLocalVariable.getAncestor(IJavaElement.TYPE), JavaElementLabelsCore.ALL_DEFAULT);
				result.addWarning(Messages.format(RefactoringCoreMessages.RenameTempRefactoring_lowercase2, new String[] { BasicElementLabels.getJavaElementName(newName), nameOfParent, nameOfType }));
			} else {
				result.addWarning(RefactoringCoreMessages.RenameTempRefactoring_lowercase);
			}
		return result;
	}

	private void createEdits() {
		TextEdit declarationEdit= createRenameEdit(fTempDeclarationNode.getName().getStartPosition());
		TextEdit[] allRenameEdits= getAllRenameEdits(declarationEdit);

		TextEdit[] allUnparentedRenameEdits= new TextEdit[allRenameEdits.length];
		TextEdit unparentedDeclarationEdit= null;

		fChange= new CompilationUnitChange(RefactoringCoreMessages.RenameTempRefactoring_rename, fCu);
		MultiTextEdit rootEdit= new MultiTextEdit();
		fChange.setEdit(rootEdit);
		fChange.setKeepPreviewEdits(true);

		for (int i= 0; i < allRenameEdits.length; i++) {
			if (fIsComposite) {
				// Add a copy of the text edit (text edit may only have one
				// parent) to keep problem reporting code clean
				TextChangeCompatibility.addTextEdit(fChangeManager.get(fCu), RefactoringCoreMessages.RenameTempRefactoring_changeName, allRenameEdits[i].copy(), fCategorySet);

				// Add a separate copy for problem reporting
				allUnparentedRenameEdits[i]= allRenameEdits[i].copy();
				if (allRenameEdits[i].equals(declarationEdit))
					unparentedDeclarationEdit= allUnparentedRenameEdits[i];
			}
			rootEdit.addChild(allRenameEdits[i]);
			fChange.addTextEditGroup(new TextEditGroup(RefactoringCoreMessages.RenameTempRefactoring_changeName, allRenameEdits[i]));
		}

		// store information for analysis
		if (fIsComposite) {
			fLocalAnalyzePackage= new RenameAnalyzeUtil.LocalAnalyzePackage(unparentedDeclarationEdit, allUnparentedRenameEdits);
		} else
			fLocalAnalyzePackage= new RenameAnalyzeUtil.LocalAnalyzePackage(declarationEdit, allRenameEdits);
	}

	private TextEdit[] getAllRenameEdits(TextEdit declarationEdit) {
		if (! fUpdateReferences)
			return new TextEdit[] { declarationEdit };

		TempOccurrenceAnalyzer fTempAnalyzer= new TempOccurrenceAnalyzer(fTempDeclarationNode, true);
		fTempAnalyzer.perform();
		int[] referenceOffsets= fTempAnalyzer.getReferenceAndJavadocOffsets();

		TextEdit[] allRenameEdits= new TextEdit[referenceOffsets.length + 1];
		for (int i= 0; i < referenceOffsets.length; i++)
			allRenameEdits[i]= createRenameEdit(referenceOffsets[i]);
		allRenameEdits[referenceOffsets.length]= declarationEdit;
		return allRenameEdits;
	}

	private TextEdit createRenameEdit(int offset) {
		return new ReplaceEdit(offset, fCurrentName.length(), fNewName);
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.RenameTypeProcessor_creating_changes, 1);

			RenameJavaElementDescriptor descriptor= createRefactoringDescriptor();
			fChange.setDescriptor(new RefactoringChangeDescriptor(descriptor));
			return fChange;
		} finally {
			monitor.done();
		}
	}

	private RenameJavaElementDescriptor createRefactoringDescriptor() {
		String project= null;
		IJavaProject javaProject= fCu.getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		final String header= Messages.format(RefactoringCoreMessages.RenameLocalVariableProcessor_descriptor_description, new String[] { BasicElementLabels.getJavaElementName(fCurrentName), JavaElementLabelsCore.getElementLabel(fLocalVariable.getParent(), JavaElementLabelsCore.ALL_FULLY_QUALIFIED), BasicElementLabels.getJavaElementName(fNewName)});
		final String description= Messages.format(RefactoringCoreMessages.RenameLocalVariableProcessor_descriptor_description_short, BasicElementLabels.getJavaElementName(fCurrentName));
		final String comment= new JDTRefactoringDescriptorComment(project, this, header).asString();
		final RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_LOCAL_VARIABLE);
		descriptor.setProject(project);
		descriptor.setDescription(description);
		descriptor.setComment(comment);
		descriptor.setFlags(RefactoringDescriptor.NONE);
		descriptor.setJavaElement(fLocalVariable);
		descriptor.setNewName(getNewElementName());
		descriptor.setUpdateReferences(fUpdateReferences);
		return descriptor;
	}

	private RefactoringStatus initialize(JavaRefactoringArguments extended) {
		final String handle= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
			if (element != null && element.exists()) {
				if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
					fCu= (ICompilationUnit) element;
				} else if (element.getElementType() == IJavaElement.LOCAL_VARIABLE) {
					fLocalVariable= (ILocalVariable) element;
					fCu= (ICompilationUnit) fLocalVariable.getAncestor(IJavaElement.COMPILATION_UNIT);
					if (fCu == null)
						return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(), IJavaRefactorings.RENAME_LOCAL_VARIABLE);
				} else
					return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(), IJavaRefactorings.RENAME_LOCAL_VARIABLE);
			} else
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(), IJavaRefactorings.RENAME_LOCAL_VARIABLE);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
		final String name= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME);
		if (name != null && !"".equals(name)) //$NON-NLS-1$
			setNewElementName(name);
		else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME));
		if (fCu != null && fLocalVariable == null) {
			final String selection= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION);
			if (selection != null) {
				int offset= -1;
				int length= -1;
				final StringTokenizer tokenizer= new StringTokenizer(selection);
				if (tokenizer.hasMoreTokens())
					offset= Integer.parseInt(tokenizer.nextToken());
				if (tokenizer.hasMoreTokens())
					length= Integer.parseInt(tokenizer.nextToken());
				if (offset >= 0 && length >= 0) {
					try {
						final IJavaElement[] elements= fCu.codeSelect(offset, length);
						if (elements != null) {
							for (IJavaElement element : elements) {
								if (element instanceof ILocalVariable)
									fLocalVariable= (ILocalVariable) element;
							}
						}
						if (fLocalVariable == null)
							return JavaRefactoringDescriptorUtil.createInputFatalStatus(null, getProcessorName(), IJavaRefactorings.RENAME_LOCAL_VARIABLE);
					} catch (JavaModelException exception) {
						JavaManipulationPlugin.log(exception);
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection,
							JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION }));
			} else
				return RefactoringStatus
						.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION));
		}
		final String references= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_REFERENCES);
		if (references != null) {
			fUpdateReferences= Boolean.parseBoolean(references);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_REFERENCES));
		return new RefactoringStatus();
	}

	public RenameAnalyzeUtil.LocalAnalyzePackage getLocalAnalyzePackage() {
		return fLocalAnalyzePackage;
	}
}
