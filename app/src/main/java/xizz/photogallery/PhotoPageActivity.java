package xizz.photogallery;

import android.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActivity {
	@Override
	protected Fragment createFragment() {
		return new PhotoPageFragment();
	}
}
