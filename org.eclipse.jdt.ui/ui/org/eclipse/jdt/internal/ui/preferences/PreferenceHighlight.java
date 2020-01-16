package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public final class PreferenceHighlight implements PaintListener {

	private static final int HIGHLIGHT_FOCUS= SWT.COLOR_WIDGET_DARK_SHADOW;

	private static final int HIGHLIGHT_MOUSE= SWT.COLOR_WIDGET_NORMAL_SHADOW;

	private final Composite fParent;

	private final Control fLabelControl;

	private final Control fMainControl;

	private boolean fHover;

	private boolean fFocus;

	private PreferenceHighlight(Composite parent, Control labelControl, Control mainControl) {
		fParent= parent;
		fLabelControl= labelControl;
		fMainControl= mainControl;
	}

	@Override
	public void paintControl(PaintEvent e) {
		if ((!fHover && !fFocus) || !fMainControl.isEnabled() || ((GridData) fLabelControl.getLayoutData()).exclude)
			return;

		final int GAP= 7;
		final int ARROW= 3;
		Rectangle l= fLabelControl.getBounds();
		Point c= fMainControl.getLocation();

		e.gc.setForeground(e.display.getSystemColor(fFocus ? HIGHLIGHT_FOCUS : HIGHLIGHT_MOUSE));
		int x2= c.x - GAP;
		int y= l.y + l.height / 2 + 1;

		e.gc.drawLine(l.x + l.width + GAP, y, x2, y);
		e.gc.drawLine(x2 - ARROW, y - ARROW, x2, y);
		e.gc.drawLine(x2 - ARROW, y + ARROW, x2, y);
	}

	/**
	 * Adds the highlight feature to a pair of controls, that is an arrow painted between the label
	 * and the main control for easier selection and to show when the control has focus. Both
	 * controls must have the same parent.
	 *
	 * @param labelControl the control acting as label
	 * @param mainControl the main control
	 * @param autoFocus if true, focus highlight will be switched automatically based on main
	 *            controls focus state
	 * @return highlight handler
	 */
	public static PreferenceHighlight addHighlight(Control labelControl, Control mainControl, boolean autoFocus) {
		final Composite parent= labelControl.getParent();
		PreferenceHighlight highlight= new PreferenceHighlight(parent, labelControl, mainControl);
		parent.addPaintListener(highlight);

		if (autoFocus) {
			mainControl.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) {
					highlight.setFocus(false);
				}

				@Override
				public void focusGained(FocusEvent e) {
					highlight.setFocus(true);
				}
			});
		}

		MouseTrackAdapter labelComboListener= new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				highlight.setHover(true);
			}

			@Override
			public void mouseExit(MouseEvent e) {
				highlight.setHover(false);
			}
		};
		mainControl.addMouseTrackListener(labelComboListener);
		labelControl.addMouseTrackListener(labelComboListener);

		parent.addMouseMoveListener(e -> {
			highlight.setHover(highlight.isAroundLabel(e));
		});
		parent.addMouseTrackListener(MouseTrackListener.mouseExitAdapter(e -> {
			highlight.setHover(false);
		}));
		parent.addMouseListener(MouseListener.mouseDownAdapter(e -> {
			if (highlight.isAroundLabel(e))
				mainControl.setFocus();
		}));

		if (labelControl instanceof Label) {
			labelControl.addMouseListener(MouseListener.mouseDownAdapter(e -> mainControl.setFocus()));
		}

		return highlight;
	}

	public void setFocus(boolean focus) {
		if (fFocus != focus) {
			fFocus= focus;
			fParent.redraw();
		}
	}

	public void setHover(boolean hover) {
		if (fHover != hover) {
			fHover= hover;
			fParent.redraw();
		}
	}

	private boolean isAroundLabel(MouseEvent e) {
		int lx= fLabelControl.getLocation().x;
		Rectangle c= fMainControl.getBounds();
		int x= e.x;
		int y= e.y;
		return lx - 5 < x && x < c.x && c.y - 2 < y && y < c.y + c.height + 2;
	}
}
