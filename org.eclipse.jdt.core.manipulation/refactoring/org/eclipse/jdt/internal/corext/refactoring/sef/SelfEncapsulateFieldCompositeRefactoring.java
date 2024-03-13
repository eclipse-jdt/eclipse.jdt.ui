/*******************************************************************************
 * Copyright (c) 2024 Yatta.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yatta - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.sef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.refactoring.descriptors.EncapsulateFieldDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTesterCore;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.Messages;

public class SelfEncapsulateFieldCompositeRefactoring extends Refactoring {

	private final List<SelfEncapsulateFieldRefactoring> fRefactorings;
	private final HashMap<ICompilationUnit, ImportRewrite> fImportRewritesMap = new HashMap<>();
	private final HashMap<ICompilationUnit, ASTRewrite> fRewritersMap = new HashMap<>();
	private final HashMap<ICompilationUnit, CompilationUnit> fRootsMap = new HashMap<>();
	private final HashMap<ICompilationUnit, List<TextEditGroup>> fDescriptionsMap = new HashMap<>();
	private final List<TextChange> changes = new ArrayList<>();

	public List<SelfEncapsulateFieldRefactoring> getRefactorings() {
		return fRefactorings;
	}

	public SelfEncapsulateFieldCompositeRefactoring(List<IField> fields) throws JavaModelException {
		fRefactorings = getRefactoringFromFields(fields);
	}

	public List<SelfEncapsulateFieldRefactoring> getRefactoringFromFields(List<IField> fields) throws JavaModelException {
		List<SelfEncapsulateFieldRefactoring> refactorings = new ArrayList<>();
		for(IField field : fields) {
			if (RefactoringAvailabilityTesterCore.isSelfEncapsulateAvailable(field)) {
				refactorings.add(new SelfEncapsulateFieldRefactoring(field, this));
			}
		}
		return refactorings;
	}

	@Override
	public String getName() {
		return RefactoringCoreMessages.SelfEncapsulateField_name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		RefactoringStatus fInitialConditions = new RefactoringStatus();
		for(SelfEncapsulateFieldRefactoring selfEncapsulateFieldRefactoring : fRefactorings) {
			fInitialConditions.merge(selfEncapsulateFieldRefactoring.checkInitialConditions(pm));
		}
		return fInitialConditions;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask(RefactoringCoreMessages.SelfEncapsulateField_create_changes, changes.size());
		final DynamicValidationRefactoringChange result= new DynamicValidationRefactoringChange(getDescriptor(), getName());
		for (TextChange change : changes) {
			result.add(change);
			pm.worked(1);
		}
		pm.done();
		return result;
	}

	public EncapsulateFieldDescriptor getDescriptor () {
		final Map<String, String> arguments= new HashMap<>();
		String project= null;
		IJavaProject javaProject= getSelectedRefactorings().get(0).getField().getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		String fieldLabels = (getSelectedRefactorings().size() <= 3
				? getSelectedRefactorings()
				: getSelectedRefactorings().subList(0, 3)).stream().map(refactoring -> refactoring.getField().getElementName()).collect(Collectors.joining(", ")) + ", ..."; //$NON-NLS-1$ //$NON-NLS-2$;
		final String description= Messages.format(RefactoringCoreMessages.SelfEncapsulateField_descriptor_description_short, fieldLabels);
		int flags= JavaRefactoringDescriptor.JAR_MIGRATION | JavaRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
		try {
			IType declaring = getSelectedRefactorings().get(0).getField().getDeclaringType();
			boolean isAnonymousOrLocal = declaring.isAnonymous() || declaring.isLocal();
			if(isAnonymousOrLocal) {
				flags|= JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
			}
		} catch (JavaModelException exception) {
			JavaManipulationPlugin.log(exception);
		}
		String comment = getSelectedRefactorings().stream().map(refactoring -> refactoring.getCommentsForDescriptor().asString()).collect(Collectors.joining(", ")); //$NON-NLS-1$
		getSelectedRefactorings().forEach(refactoring -> refactoring.getArgumentsForDescriptor().forEach((key, value) -> arguments.merge(key, value, (v1, v2) -> v1 + ", " + v2))); //$NON-NLS-1$
		final EncapsulateFieldDescriptor descriptor= RefactoringSignatureDescriptorFactory.createEncapsulateFieldDescriptor(project, description, comment, arguments, flags);

		return descriptor;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		clearResources();
		RefactoringStatus fFinalConditions = new RefactoringStatus();
		for(SelfEncapsulateFieldRefactoring selfEncapsulateFieldRefactoring : getSelectedRefactorings()) {
			fFinalConditions.merge(selfEncapsulateFieldRefactoring.checkFinalConditions(pm));
		}
		makeDeclarationsPrivate();
		TextChangeManager fChangeManager = new TextChangeManager();
		for(Entry<ICompilationUnit, ASTRewrite> entry : fRewritersMap.entrySet()) {
			TextChange change= fChangeManager.get(entry.getKey());
			MultiTextEdit root= new MultiTextEdit();
			change.setEdit(root);
			root.addChild(fImportRewritesMap.get(entry.getKey()).rewriteImports(null));
			root.addChild(entry.getValue().rewriteAST());
			for (TextEditGroup textEditGroup : fDescriptionsMap.get(entry.getKey())) {
				change.addTextEditGroup(textEditGroup);
			}
			changes.add(change);
		}
		return fFinalConditions;
	}

	private void makeDeclarationsPrivate() throws CoreException {
		for(SelfEncapsulateFieldRefactoring refactoring : getSelectedRefactorings()) {
			refactoring.makeDeclarationPrivateIfNeeded();
		}
	}

	public void clearResources() {
		fImportRewritesMap.clear();
		fRewritersMap.clear();
		fRootsMap.clear();
		fDescriptionsMap.clear();
		changes.clear();
	}


	public ASTRewrite getRewriterForUnit(ICompilationUnit cu, CompilationUnit root) {
		ASTRewrite rewriter;
		if(fRewritersMap.get(cu) == null) {
			rewriter = ASTRewrite.create(root.getAST());
			fRewritersMap.put(cu, rewriter);
		} else {
			rewriter = fRewritersMap.get(cu);
		}
		return rewriter;
	}

	public ImportRewrite getImportRewriterForUnit(ICompilationUnit cu, CompilationUnit root) {
		ImportRewrite importRewrite;
		if(fImportRewritesMap.get(cu) == null) {
			importRewrite = StubUtility.createImportRewrite(root, true);
			fImportRewritesMap.put(cu, importRewrite);
		} else {
			importRewrite = fImportRewritesMap.get(cu);
		}
		return importRewrite;
	}

	public CompilationUnit getRootForUnit(ICompilationUnit cu) {
		CompilationUnit root;
		if(fRootsMap.get(cu) == null) {
			root= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL).parse(cu, true);
			fRootsMap.put(cu, root);
		} else {
			root = fRootsMap.get(cu);
		}
		return root;
	}

	public List<TextEditGroup> getDescriptionsForUnit(ICompilationUnit cu) {
		List<TextEditGroup> descriptions;
		if(fDescriptionsMap.get(cu) == null) {
			descriptions= new ArrayList<>();
			fDescriptionsMap.put(cu, descriptions);
		} else {
			descriptions = fDescriptionsMap.get(cu);
		}
		return descriptions;
	}

	public List<SelfEncapsulateFieldRefactoring> getSelectedRefactorings() {
		return fRefactorings.stream().filter(SelfEncapsulateFieldRefactoring::isSelected).toList();
	}

}
