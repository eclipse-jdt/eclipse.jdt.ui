package p; //remove 'part'

import java.util.Vector;

class HistoryFrame extends Vector {

	public HistoryFrame(int title) {

		super(title);

	}
}
class EvaViewPart {

	private void showJFreeChartFrame() {
		HistoryFrame frame= new HistoryFrame(5);
	}

}