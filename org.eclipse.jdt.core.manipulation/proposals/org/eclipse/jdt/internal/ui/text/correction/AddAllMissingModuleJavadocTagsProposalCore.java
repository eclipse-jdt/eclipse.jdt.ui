package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.UsesDirective;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;

import org.eclipse.jdt.internal.ui.JavaUIStatus;

public class AddAllMissingModuleJavadocTagsProposalCore extends CUCorrectionProposalCore {

	private final ModuleDeclaration fDecl; // MethodDecl or TypeDecl or ModuleDecl
	private final ASTNode fMissingNode;

	public AddAllMissingModuleJavadocTagsProposalCore(String label, ICompilationUnit cu, ModuleDeclaration decl, ASTNode missingNode, int relevance) {
		super(label, cu, null, relevance);
		fDecl= decl;
		fMissingNode= missingNode;
	}

	@Override
	public void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
		try {
			Javadoc javadoc= null;
			int insertPosition;
			String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
			final ICompilationUnit unit= getCompilationUnit();
			CompilationUnit cu= (CompilationUnit)fDecl.getParent();
			Name moduleName= fDecl.getName();
			List<Comment> comments= cu.getCommentList();
			for (Comment comment : comments) {
				if (comment instanceof Javadoc
						&& comment.getStartPosition() + comment.getLength() < moduleName.getStartPosition()) {
						javadoc= (Javadoc)comment;
				}
			}
			if (javadoc == null) {
				return;
			}
			StringBuilder comment= new StringBuilder();
			insertPosition= AddMissingModuleJavadocTagProposalCore.findInsertPosition(javadoc, fMissingNode, document, lineDelimiter);

		 	List<ModuleDirective> moduleStatements= fDecl.moduleStatements();
		 	for (int i= moduleStatements.size() - 1; i >= 0 ; i--) {
		 		ModuleDirective directive= moduleStatements.get(i);
		 		String name;
		 		if (directive instanceof ProvidesDirective) {
		 			name= ((ProvidesDirective)directive).getName().getFullyQualifiedName().toString();
		 			if (JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_PROVIDES, name) == null) {
				 		comment.append(" * ").append(TagElement.TAG_PROVIDES).append(" ").append(name).append(lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
		 			}
		 		}
		 	}

		 	for (int i= moduleStatements.size() - 1; i >= 0 ; i--) {
		 		ModuleDirective directive= moduleStatements.get(i);
		 		String name;
		 		if (directive instanceof UsesDirective) {
		 			name= ((UsesDirective)directive).getName().getFullyQualifiedName().toString();
		 			if (JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_USES, name) == null) {
				 		comment.append(" * ").append(TagElement.TAG_USES).append(" ").append(name).append(lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
		 			}
		 		}
		 	}

			IRegion region= document.getLineInformationOfOffset(insertPosition);

			String lineContent= document.get(region.getOffset(), region.getLength());
			String indentString= Strings.getIndentString(lineContent, unit);
			String str= Strings.changeIndent(comment.toString(), 0, unit, indentString, lineDelimiter);
			InsertEdit edit= new InsertEdit(insertPosition, str);
			rootEdit.addChild(edit);
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}
	}
}