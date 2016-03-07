package DSC.android.services.DSCcoreserviceproxy;

public interface DSCCoreServiceListener {

	public void onResult(byte[] image); // Called when we have Image data

	public void onResult(int result); // Called when we have get call returning
										// a value

	public void onError(String text); // Called when a set/get call has failed

}