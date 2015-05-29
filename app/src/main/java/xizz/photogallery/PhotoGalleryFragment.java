package xizz.photogallery;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	ThumbnailDownloader<ImageView> mThumbnailThread;
	private GridView mGridView;
	private List<GalleryItem> mItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);

		updateItems();

		mThumbnailThread = new ThumbnailDownloader<>(new Handler());
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
			@Override
			public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
				if (isVisible())
					imageView.setImageBitmap(thumbnail);
			}
		});
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		mGridView = (GridView) v.findViewById(R.id.gridView);

		setupAdapter();

		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailThread.quit();
		Log.d(TAG, "Background thread destroyed");
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailThread.clearQueue();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_photo_gallery, menu);
		// pull out the SearchView
//		MenuItem searchItem = menu.findItem(R.id.menu_item_search);
//		SearchView searchView = (SearchView) searchItem.getActionView();
//
//		// get the data from our searchable.xml as a SearchableInfo
//		SearchManager searchManager = (SearchManager) getActivity()
//				.getSystemService(Context.SEARCH_SERVICE);
//		ComponentName name = getActivity().getComponentName();
//		SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
//
//		searchView.setSearchableInfo(searchInfo);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_search:
				getActivity().onSearchRequested();
				return true;
			case R.id.menu_item_clear:
				PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
						.putString(FlickrFetchr.PREF_SEARCH_QUERY, null).commit();
				updateItems();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void updateItems() {
		new FetchItemsTask().execute();
	}

	private void setupAdapter() {
		if (getActivity() == null || mGridView == null)
			return;

		if (mItems != null)
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
		else
			mGridView.setAdapter(null);
	}

	private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
		@Override
		protected List<GalleryItem> doInBackground(Void... params) {
			Activity activity = getActivity();
			if (activity == null)
				return new ArrayList<>();

			String query = PreferenceManager.getDefaultSharedPreferences(activity)
					.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);

			if (query != null)
				return new FlickrFetchr().search(query);
			else
				return new FlickrFetchr().fetchItems();
		}

		@Override
		protected void onPostExecute(List<GalleryItem> galleryItems) {
			mItems = galleryItems;
			setupAdapter();
		}
	}

	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
		public GalleryItemAdapter(List<GalleryItem> items) {
			super(getActivity(), 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.gallery_item, parent, false);

			ImageView imageView =
					(ImageView) convertView.findViewById(R.id.gallery_item_imageView);
			imageView.setImageResource(R.mipmap.ic_launcher);

			GalleryItem item = getItem(position);
			mThumbnailThread.queueThumbnail(imageView, item.url);

			return convertView;
		}
	}
}
