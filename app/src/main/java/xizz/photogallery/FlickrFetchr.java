package xizz.photogallery;

import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
	public static final String TAG = "FlickrFetchr";
	public static final String PREF_SEARCH_QUERY = "searchQuery";
	public static final String PREF_LAST_RESULT_ID = "lastResultId";

	private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
	private static final String API_KEY = "80b97a505513dac3efd2081401cdd82e";
	private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
	private static final String METHOD_SEARCH = "flickr.photos.search";
	private static final String PARAM_EXTRAS = "extras";
	private static final String PARAM_TEXT = "text";

	private static final String EXTRA_SMALL_URL = "url_s";

	private static final String XML_PHOTO = "photo";

	public List<GalleryItem> fetchItems() {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_GET_RECENT)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.build().toString();
		return downloadGalleryItems(url);
	}

	public List<GalleryItem> search(String query) {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_SEARCH)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PARAM_TEXT, query)
				.build().toString();
		return downloadGalleryItems(url);
	}

	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}

	public byte[] getUrlBytes(String urlSpec) throws IOException {
		HttpURLConnection connection = null;
		InputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			URL url = new URL(urlSpec);
			connection = (HttpURLConnection) url.openConnection();
			out = new ByteArrayOutputStream();
			in = connection.getInputStream();

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				return new byte[0];

			int bytesRead;
			byte[] buffer = new byte[1024];
			while ((bytesRead = in.read(buffer)) > 0)
				out.write(buffer, 0, bytesRead);
			return out.toByteArray();
		} finally {
			if (connection != null)
				connection.disconnect();
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}

	private List<GalleryItem> downloadGalleryItems(String url) {
		List<GalleryItem> items = new ArrayList<>();

		try {
			String xmlString = getUrl(url);
			Log.d(TAG, "Received xml: " + xmlString);
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));

			parseItems(items, parser);
		} catch (IOException ioe) {
			Log.e(TAG, "Failed to fetch items", ioe);
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "Failed to parse items", xppe);
		}
		return items;
	}

	private void parseItems(List<GalleryItem> items, XmlPullParser parser)
			throws XmlPullParserException, IOException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
				String owner = parser.getAttributeValue(null, "owner");

				GalleryItem item = new GalleryItem();
				item.id = id;
				item.caption = caption;
				item.url = smallUrl;
				item.owner = owner;
				items.add(item);
			}

			eventType = parser.next();
		}
	}
}
