package xizz.photogallery;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import java.io.IOException;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";

	private GridView mGridView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		new FetchItemsTask().execute();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		mGridView = (GridView) v.findViewById(R.id.gridView);
		return v;
	}

	private class FetchItemsTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				String result = new FlickrFetchr().getUrl("http://www.google.com");
				Log.d(TAG, "Fetched contents of URL: " + result);
			} catch (IOException e) {
				Log.e(TAG, "Failed to fetch URL: ", e);
			}
			return null;
		}
	}
}
