package org.eclipse.jdt.ui.tests.search;

import java.util.Random;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;

/**
 */
public class TestParticipant implements IQueryParticipant {
	private static final Random fgRandom= new Random();
	private IMatchPresentation fUIParticipant;

	public TestParticipant() {
		fUIParticipant= new TestUIParticipant();
	}
	
	public void search(ISearchRequestor requestor, QuerySpecification data, IProgressMonitor monitor) throws CoreException {
		for (int i= 0; i < 20; i++) {
			requestor.reportMatch(new Match(new Integer(fgRandom.nextInt()), -1, -1));
		}
	}

	public int estimateTicks(QuerySpecification data) {
		return 100;
	}

	public IMatchPresentation getUIParticipant() {
		return fUIParticipant;
	}

}

class TestLabelProvider extends LabelProvider {
	public Image getImage(Object element) {
		return JavaPluginImages.get(ISharedImages.IMG_OBJS_PROTECTED);
	}
	public String getText(Object element) {
		return "Int: value= "+((Integer)element).toString();
	}
}

class TestUIParticipant implements IMatchPresentation {

	public void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		MessageDialog.openInformation(null, "Showing element", "Showning an integer of value: "+((Integer)match.getElement()).intValue());
		
	}

	public ILabelProvider createLabelProvider() {
		return new TestLabelProvider();
	}

}
