package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class UnresolvedElementsSubProcessor {
	
	
	public static void getVariableProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName node= (SimpleName) selectedNode;


		// corrections
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, node, SimilarElementsRequestor.VARIABLES);
		for (int i= 0; i < elements.length; i++) {
			SimilarElement curr= elements[i];
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changevariable.description", curr.getName()); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(label, problemPos, curr.getName(), 3));
		}
		
		// new field
		IJavaElement elem= cu.getElementAt(problemPos.getOffset());
		if (elem instanceof IMember) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.description", node.getIdentifier()); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal(label, NewVariableCompletionProposal.FIELD, node, (IMember) elem, 2));
		}
		if (elem instanceof IMethod) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createlocal.description", node.getIdentifier()); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal(label, NewVariableCompletionProposal.LOCAL, node, (IMember) elem, 1));
		
			label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createparameter.description", node.getIdentifier()); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal(label, NewVariableCompletionProposal.PARAM, node, (IMember) elem, 1));
		}			
		
		if (node.getParent().getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation invocation= (MethodInvocation) node.getParent();
			if (node.equals(invocation.getExpression())) {
				getTypeProposals(problemPos, SimilarElementsRequestor.REF_TYPES, proposals);
			}
		}
	}
	
	public static void getTypeProposals(ProblemPosition problemPos, int kind, ArrayList proposals) throws CoreException {
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		// corrections
		String typeName= cu.getBuffer().getText(problemPos.getOffset(), problemPos.getLength());
		int bracketIndex= typeName.indexOf('[');
		if (bracketIndex != -1) {
			typeName= typeName.substring(0, bracketIndex);
		}
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, problemPos.getOffset(), typeName, kind);
		for (int i= 0; i < elements.length; i++) {
			String curr= elements[i].getName();
			
			ImportEdit importEdit= new ImportEdit(cu, settings);
			importEdit.addImport(curr);
			
			String simpleName= Signature.getSimpleName(curr);
			boolean importOnly= simpleName.equals(typeName);
			
			CUCorrectionProposal proposal= new CUCorrectionProposal("", cu, 0); //$NON-NLS-1$
			proposals.add(proposal);
			
			CompilationUnitChange change= proposal.getCompilationUnitChange();
			
			if (!importEdit.isEmpty()) {
				change.addTextEdit("import", importEdit); //$NON-NLS-1$
			}
			if (!importOnly) {
				change.addTextEdit("change", SimpleTextEdit.createReplace(problemPos.getOffset(), typeName.length(), simpleName)); //$NON-NLS-1$
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.description", simpleName)); //$NON-NLS-1$
				proposal.setRelevance(3);
			} else {
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.importtype.description", curr)); //$NON-NLS-1$
				proposal.setRelevance(5);
			}
		}
		
		// add type
		String addedCUName= typeName + ".java"; //$NON-NLS-1$
		if (!JavaConventions.validateCompilationUnitName(addedCUName).matches(IStatus.ERROR)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			final ICompilationUnit addedCU= pack.getCompilationUnit(addedCUName);
			if (!addedCU.exists()) {
				if ((kind & SimilarElementsRequestor.CLASSES) != 0) {
					String[] superTypes= (problemPos.getId() != IProblem.ExceptionTypeNotFound) ? null : new String[] { "java.lang.Exception" }; //$NON-NLS-1$
					String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createclass.description", typeName); //$NON-NLS-1$
					proposals.add(new NewCUCompletionProposal(label, addedCU, true, superTypes, 0));
				}
				if ((kind & SimilarElementsRequestor.INTERFACES) != 0) {
					String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createinterface.description", typeName); //$NON-NLS-1$
					proposals.add(new NewCUCompletionProposal(label, addedCU, false, null, 0));
				}				
			}
		}
	}
	
	public static void getMethodProposals(ProblemPosition problemPos, boolean needsNewName, ArrayList proposals) throws CoreException {

		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName nameNode= (SimpleName) selectedNode;
		
		if (!(selectedNode.getParent() instanceof MethodInvocation)) {
			return;
		}
		MethodInvocation invocationNode= (MethodInvocation) nameNode.getParent();
		
		// corrections
		String methodName= nameNode.getIdentifier();
				
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, nameNode, SimilarElementsRequestor.METHODS);
		for (int i= 0; i < elements.length; i++) {
			String curr= elements[i].getName();
			if (curr.equals(methodName) && needsNewName) {
				continue;
			}
			
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changemethod.description", curr); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(label, problemPos, curr, 2));
		}
		
		// new method
		
		// evaluate sender
		IType type= null;
		Expression sender= invocationNode.getExpression();
		if (sender != null) {
			ITypeBinding binding= sender.resolveTypeBinding();
			if (binding != null && binding.isFromSource()) {
				type= Binding2JavaModel.find(binding, cu.getJavaProject());
				if (type != null) {
					ICompilationUnit changedCU= type.getCompilationUnit();
					if (!changedCU.isWorkingCopy()) {
						changedCU= EditorUtility.getWorkingCopy(changedCU);
						if (changedCU != null) {
							type= (IType) JavaModelUtil.findMemberInCompilationUnit(changedCU, type);
						}
					}
				}
			}
		} else {
			IJavaElement elem= cu.getElementAt(problemPos.getOffset());
			if (elem != null) {
				type= (IType) elem.getAncestor(IJavaElement.TYPE);
			}
		}
				
		if (type != null) {	
			String label;
			if (cu.equals(type.getCompilationUnit())) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.description", methodName); //$NON-NLS-1$
			} else {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.other.description", new Object[] { methodName, type.getElementName() } ); //$NON-NLS-1$
			}
			proposals.add(new NewMethodCompletionProposal(label, invocationNode, cu, type, 1));
		}
	}
	

}
