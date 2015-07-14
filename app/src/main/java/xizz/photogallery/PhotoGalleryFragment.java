package xizz.photogallery;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {
	private static final String TAG = "PhotoGalleryFragment";
	private ThumbnailDownloader<ImageView> mThumbnailThread;
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
		Log.d(TAG, "Background thread started");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		mGridView = (GridView) v.findViewById(R.id.gridView);

		setupAdapter();

		mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				GalleryItem item = mItems.get(position);

				Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
				Intent i = new Intent(getActivity(), PhotoPageActivity.class);
				i.setData(photoPageUri);

				startActivity(i);
			}
		});

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
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		if (PollService.isServiceAlarmOn(getActivity()))
			toggleItem.setTitle(R.string.stop_polling);
		else
			toggleItem.setTitle(R.string.start_polling);
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
			case R.id.menu_item_toggle_polling:
				boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
				PollService.setServiceAlarm(getActivity(), shouldStartAlarm);

				getActivity().invalidateOptionsMenu();

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
