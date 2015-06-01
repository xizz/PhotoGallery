package xizz.photogallery;

public class GalleryItem {
	public String caption;
	public String id;
	public String url;
	public String owner;

	public String getPhotoPageUrl() {
		return "https://www.flickr.com/photos/" + owner + "/" + id;
	}

	@Override
	public String toString() {
		return caption;
	}
}
