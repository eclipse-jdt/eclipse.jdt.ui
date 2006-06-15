package receiver_out;

public class TestRemoteFieldReceiver {
	RFRTarget fTarget;

	void toInline() {
		TestRemoteFieldReceiver i= this.fTarget.fId;
		TestRemoteFieldReceiver j= fTarget.fId;
		RFRTarget k= fTarget.fId.fTarget.fId.fTarget;
	}
}

class RFRTarget {
	public TestRemoteFieldReceiver fId;
}

class RFRCaller {
	TestRemoteFieldReceiver fX;

	public void analyseCode() {
		TestRemoteFieldReceiver i= fX.fTarget.fId;
		TestRemoteFieldReceiver j= fX.fTarget.fId;
		RFRTarget k= fX.fTarget.fId.fTarget.fId.fTarget;
	}
}