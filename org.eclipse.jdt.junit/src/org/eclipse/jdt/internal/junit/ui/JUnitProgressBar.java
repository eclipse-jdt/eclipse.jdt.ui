package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * A progress bar with a red/green indication for success or failure.
 */
public class JUnitProgressBar extends Canvas {
	private static final int DEFAULT_WIDTH = 160;
	private static final int DEFAULT_HEIGHT = 18;

	private int fSelection= 0;
	private int fMax= 0;
	private int fX= 0;
	private boolean fError;
	
	public JUnitProgressBar(Composite parent) {
		super(parent, SWT.NONE);
		
		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				fX= scale(fX);
				redraw();
			}
		});	
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paint(e);
			}
		});
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e){
			}
		});
	}

	public void setMaximum(int max) {
		fMax= max;
	}
		
	public void reset() {
		fError= false;
		fSelection= 0;
		fX= 0;
		fMax= 0;
		redraw();
	}
	
	private void paintStep(int startX, int endX) {
		GC gc = new GC(this);	
		setStatusColor(gc);
		Rectangle rect= getClientArea();
		startX= Math.max(1, startX);
		gc.fillRectangle(startX, 1, endX-startX, rect.height-2);
		gc.dispose();		
	}

	private void setStatusColor(GC gc) {
		if (fError)
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
		else
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_GREEN));
	}

	private int scale(int value) {
		if (fMax > 0) {
			Rectangle r= getClientArea();
			if (r.width != 0)
				return Math.max(0, value*(r.width-2)/fMax);
		}
		return value; 
	}
	
	private void drawBevelRect(GC gc, int x, int y, int w, int h, Color topleft, Color bottomright) {
		gc.setForeground(topleft);
		gc.drawLine(x, y, x+w-1, y);
		gc.drawLine(x, y, x, y+h-1);
		
		gc.setForeground(bottomright);
		gc.drawLine(x+w, y, x+w, y+h);
		gc.drawLine(x, y+h, x+w, y+h);
	}
	
	private void paint(PaintEvent event) {
		GC gc = event.gc;
		Display disp= getDisplay();
			
		Rectangle rect= getClientArea();
		gc.fillRectangle(rect);
		drawBevelRect(gc, rect.x, rect.y, rect.width-1, rect.height-1,
			disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
			disp.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		
		setStatusColor(gc);
		fX= Math.min(rect.width-2, fX);
		gc.fillRectangle(1, 1, fX, rect.height-2);
	}	
	
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		Point size= null;
		size= new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		if (wHint != SWT.DEFAULT) size.x= wHint;
		if (hHint != SWT.DEFAULT) size.y= hHint;
		return size;
	}
	
	public void step(int failures) {
		fSelection++;
		int x= fX;

		fX= scale(fSelection);

		if (!fError && failures > 0) {
			fError= true;
			x= 1;
		}
		if (fSelection == fMax)
			fX= getClientArea().width-1;
		paintStep(x, fX);
	}
	
}
