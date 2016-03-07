// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

package DSC.android.services.DSCcoreserviceproxy;

import java.util.ArrayList;
import java.util.BitSet;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

public class DSCCoreServiceProxy {
	private Context mContext;
	static final int MSG_REGISTER_CLIENT = -1;
	static final int MSG_UNREGISTER_CLIENT = 0;
	static final int MSG_GET_FRAMERATE = 1;
	static final int MSG_SET_FRAMERATE = 2;
	static final int MSG_GET_RESOLUTION = 3;
	static final int MSG_SET_RESOLUTION = 4;
	static final int MSG_GET_SENSORMODE = 5;
	static final int MSG_SET_SENSORMODE = 6;
	static final int MSG_GET_DENOISING = 7;
	static final int MSG_SET_DENOISING = 8;
	static final int MSG_GET_GESTUREMAP = 9;
	static final int MSG_SET_GESTUREMAP = 10;
	static final int MSG_GET_DEPTHIMAGE = 11;
	static final int MSG_GET_CONFIDENCEIMAGE = 12;
	static final int MSG_GET_VERTEXIMAGE = 13;

	private static final String TAG = "DSCCoreServiceProxy";

	static DSCCoreServiceListener mListener;

	/** Messenger for communicating with service. */
	Messenger mService = null;

	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;

	long time1;

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DSCCoreServiceProxy.MSG_GET_FRAMERATE:
			case DSCCoreServiceProxy.MSG_GET_RESOLUTION:
			case DSCCoreServiceProxy.MSG_GET_DENOISING:
			case DSCCoreServiceProxy.MSG_GET_GESTUREMAP:
			case DSCCoreServiceProxy.MSG_GET_SENSORMODE:
				Bundle data = new Bundle();
				data = msg.getData();
				int retval = data.getInt("get_val");
				Log.d(TAG, "Got a result for a GET call, value is " + retval);
				mListener.onResult(retval);
				break;

			case DSCCoreServiceProxy.MSG_GET_DEPTHIMAGE:
			case DSCCoreServiceProxy.MSG_GET_VERTEXIMAGE:
			case DSCCoreServiceProxy.MSG_GET_CONFIDENCEIMAGE:
				long time2 = SystemClock.elapsedRealtime();
				data = new Bundle();
				data = msg.getData();
				byte[] depth_image = new byte[150 * 1024 + 1];
				depth_image = data.getByteArray("depth_image_data");
				mListener.onResult(depth_image);
				Log.d(TAG, "time taken to read 150K bytes over socket   = "
						+ (time2 - time1) + "Time1 = " + time1 + "; Time2 = "
						+ time2 + "size of depthmap received is "
						+ depth_image.length);

				/*
				 * Test code to see if we are getting the right values
				 * PrintWriter pw = null; FileOutputStream fw = null ; File
				 * depthmap = new File("/data/local/tmp/depthmap.txt") ; try {
				 * fw = new FileOutputStream(depthmap);
				 * fw.write(depth_image,0,depth_image.length -1); fw.close(); }
				 * catch (IOException e) { // TODO Auto-generated catch block
				 * e.printStackTrace(); }
				 */
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	public void setListener(DSCCoreServiceListener l) {
		mListener = l;
	}

	/**
	 * Class for interacting with the main interfaces of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null,
						DSCCoreServiceProxy.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);

			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
			}

			// As part of the sample, tell the user what happened.
			mIsBound = true;
			Log.d(TAG, "DSCCoreServiceProxy connected to Service");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			mIsBound = false;
			Log.d(TAG, "DSCCoreServiceProxy disconnected to Service");
		}
	};

	public boolean isBound() {
		return mIsBound;
	}

	public void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		Intent serviceintent = new Intent(
				"DSC.samples.DSCcoreandroidservice.DSCCoreAndroidService");
		mContext.bindService(serviceintent, mConnection,
				Context.BIND_AUTO_CREATE);
		Log.d(TAG, "Binding");

	}

	public void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							DSCCoreServiceProxy.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}

			// Detach our existing connection.
			mContext.unbindService(mConnection);
			mIsBound = false;
			Log.d(TAG, "Unbinding");
		}
	}

	public DSCCoreServiceProxy(Context ctx) {
		this.mContext = ctx;
	}

	private void sendCommandDownstream(TouchCommandParams tc) {

		Bundle b = new Bundle();
		for (int i = 2; i < tc.size(); i++) {
			b.putInt("param" + (i - 1), (Integer) tc.get(i));
		}

		Message msg = Message.obtain(null, (Integer) tc.get(1));
		if (tc.size() > 2) {
			msg.setData(b);
		}

		try {
			mService.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setFrameRate(int val) {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mSetFrameRate);
		tc.addParam(new Integer(val));
		sendCommandDownstream(tc);
	}

	public void getFrameRate() {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mGetFrameRate);
		sendCommandDownstream(tc);
	}

	public void setResolution(int val) {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mSetResolution);
		tc.addParam(new Integer(val));
		sendCommandDownstream(tc);
	}

	public void getResolution() {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mGetResolution);
		sendCommandDownstream(tc);
	}

	public void setSensorMode(int val) {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mSetSensorMode);
		tc.addParam(new Integer(val));
		sendCommandDownstream(tc);

	}

	public void getSensorMode() {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mGetSensorMode);
		sendCommandDownstream(tc);
	}

	public void setGestureMap(BitSet bs) {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mSetGestureMap);
		tc.addParam(bs);
		sendCommandDownstream(tc);
	}

	public void getGestureMap() {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mGetGestureMap);
		sendCommandDownstream(tc);
	}

	public void getDepthMap(int downSamplingRatio, int SubImageX1,
			int SubImageY1, int SubImageX2, int SubImageY2) {
		Log.d(TAG, "getDepthMap called");
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mGetDepthMap);
		tc.addParam(new Integer(downSamplingRatio));
		tc.addParam(new Integer(SubImageX1));
		tc.addParam(new Integer(SubImageY1));
		tc.addParam(new Integer(SubImageX2));
		tc.addParam(new Integer(SubImageY2));
		time1 = SystemClock.elapsedRealtime();
		sendCommandDownstream(tc);

	}

	public void getConfidenceImage(int downSamplingRatio, int SubImageX1,
			int SubImageY1, int SubImageX2, int SubImageY2) {
		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mGetConfidenceImage);
		tc.addParam(new Integer(downSamplingRatio));
		tc.addParam(new Integer(SubImageX1));
		tc.addParam(new Integer(SubImageY1));
		tc.addParam(new Integer(SubImageX2));
		tc.addParam(new Integer(SubImageY2));
		sendCommandDownstream(tc);

	}

	public void getVertexImage(int downSamplingRatio, int SubImageX1,
			int SubImageY1, int SubImageX2, int SubImageY2) {

		TouchCommandParams tc = new TouchCommandParams();
		tc.setCommand(CommandType.mGetVertexImage);
		tc.addParam(new Integer(downSamplingRatio));
		tc.addParam(new Integer(SubImageX1));
		tc.addParam(new Integer(SubImageY1));
		tc.addParam(new Integer(SubImageX2));
		tc.addParam(new Integer(SubImageY2));
		sendCommandDownstream(tc);

	}

	class CommandType {
		static final int mGetFrameRate = 1;
		static final int mSetFrameRate = 2;
		static final int mGetResolution = 3;
		static final int mSetResolution = 4;
		static final int mGetSensorMode = 5;
		static final int mSetSensorMode = 6;
		static final int mGetDenoising = 7;
		static final int mSetDenoising = 8;
		static final int mGetGestureMap = 9;
		static final int mSetGestureMap = 10;
		static final int mGetDepthMap = 11;
		static final int mGetConfidenceImage = 12;
		static final int mGetVertexImage = 13;
	}

	class TouchCommandParams extends ArrayList {
		private static final long serialVersionUID = 1L;
		boolean Repeat = false;
		int Command;
		int MagicMarker = 0xabcd;

		TouchCommandParams() {
			this.add(MagicMarker);
		}

		void setCommand(int cmd) {
			this.Command = cmd;
			this.add(new Integer(cmd));
		}

		int getCommand() {
			return this.Command;
		}

		void addParam(Object params) {
			this.add(params);
		}
	}

}
