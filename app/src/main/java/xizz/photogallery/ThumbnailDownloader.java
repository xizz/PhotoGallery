package xizz.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThumbnailDownloader<T> extends HandlerThread {
	private static final String TAG = ThumbnailDownloader.class.getSimpleName();
	private static final int MESSAGE_DOWNLOAD = 0;
	Map<T, String> mRequestMap = Collections.synchronizedMap(new HashMap<T, String>());
	private Handler mHandler;
	private Handler mResponseHandler;
	private Listener<T> mListener;

	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		mResponseHandler = responseHandler;
	}

	public void setListener(Listener<T> listener) {
		mListener = listener;
	}

	public void queueThumbnail(T token, String url) {
		Log.d(TAG, "Got an URL: " + url);
		mRequestMap.put(token, url);

		mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
	}

	public void clearQueue() {
		mHandler.removeMessages(MESSAGE_DOWNLOAD);
		mRequestMap.clear();
	}

	@Override
	protected void onLooperPrepared() {
		Log.d(TAG, "onLooperPrepared()");
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MESSAGE_DOWNLOAD) {
					T t = (T) msg.obj;
					Log.d(TAG, "Got a request for url: " + mRequestMap.get(t));
					handleRequest(t);
				}
			}
		};
	}

	private void handleRequest(final T token) {
		try {
			final String url = mRequestMap.get(token);
			if (url == null)
				return;

			byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
			final Bitmap bitmap =
					BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			Log.d(TAG, "Bitmap created");

			mResponseHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!url.equals(mRequestMap.get(token)))
						return;
					mRequestMap.remove(token);
					mListener.onThumbnailDownloaded(token, bitmap);
				}
			});
		} catch (IOException e) {
			Log.e(TAG, "Error downloading image", e);
		}
	}

	public interface Listener<T> {
		void onThumbnailDownloaded(T handle, Bitmap thumbnail);
	}
}
