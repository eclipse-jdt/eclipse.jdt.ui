package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
  */
public class LocalCorrectionsSubProcessor {

	public static void addCastProposal(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length == 2) {
			ICompilationUnit cu= problemPos.getCompilationUnit();
			String castDestType= args[1];

			CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
			ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
			
			int pos= problemPos.getOffset();
			if (selectedNode != null) {
				int parentNodeType= selectedNode.getParent().getNodeType();
				if (parentNodeType == ASTNode.ASSIGNMENT) {
					Assignment assign= (Assignment) selectedNode.getParent();
					if (selectedNode.equals(assign.getLeftHandSide())) {
						pos= assign.getRightHandSide().getStartPosition();
					}
				} else if (parentNodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
					VariableDeclarationFragment frag= (VariableDeclarationFragment) selectedNode.getParent();
					if (selectedNode.equals(frag.getName())) {
						pos= frag.getInitializer().getStartPosition();
					}
				}
			}
			
			String cast= '(' + Signature.getSimpleName(castDestType) + ')';
			String formatted= StubUtility.codeFormat(cast + 'x', 0, "");  //$NON-NLS-1$
			if (formatted.charAt(formatted.length() - 1) == 'x') {
				cast= formatted.substring(0, formatted.length() - 1);
			}
						
			String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.addcast.description", args[1]); //$NON-NLS-1$
			InsertCorrectionProposal proposal= new InsertCorrectionProposal(label, cu, pos, cast, 1);
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			ImportEdit edit= new ImportEdit(problemPos.getCompilationUnit(), settings);
			edit.addImport(castDestType);
			proposal.getCompilationUnitChange().addTextEdit("import", edit); //$NON-NLS-1$
		
			proposals.add(proposal);
		}	
	}
	
	public static void addUncaughtExceptionProposal(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		ICompilationUnit cu= problemPos.getCompilationUnit();

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		if (selectedNode == null) {
			return;
		}
		while (selectedNode != null && !(selectedNode instanceof Statement)) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode != null) {
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			SurroundWithTryCatchRefactoring refactoring= new SurroundWithTryCatchRefactoring(cu, selectedNode.getStartPosition(), selectedNode.getLength(), settings);
			refactoring.setSaveChanges(false);
			if (refactoring.checkActivationBasics(astRoot, null).isOK()) {
				String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.surroundwith.description"); //$NON-NLS-1$
				CUCorrectionProposal proposal= new CUCorrectionProposal(label, (CompilationUnitChange) refactoring.createChange(null), 0);
				proposals.add(proposal);
			}
		}
		
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl != null && decl.getNodeType() == ASTNode.METHOD_DECLARATION) {
			
			String uncaughtName= problemPos.getArguments()[0];
			
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			SimpleName name= methodDecl.getName();
			int pos= name.getStartPosition() + name.getLength();
			StringBuffer insertString= new StringBuffer();
			if (methodDecl.thrownExceptions().isEmpty()) {
				insertString.append(" throws "); //$NON-NLS-1$
			} else {
				insertString.append(", "); //$NON-NLS-1$
			}
			insertString.append(Signature.getSimpleName(uncaughtName));
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addthrows.description"); //$NON-NLS-1$
			InsertCorrectionProposal proposal= new InsertCorrectionProposal(label, cu, pos, insertString.toString(), 0);
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			ImportEdit edit= new ImportEdit(cu, settings);
			edit.addImport(uncaughtName);
			proposal.getCompilationUnitChange().addTextEdit("import", edit); //$NON-NLS-1$
		
			proposals.add(proposal);
		}
	}

}
