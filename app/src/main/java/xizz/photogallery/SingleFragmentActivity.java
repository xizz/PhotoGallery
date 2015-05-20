package xizz.photogallery;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

public abstract class SingleFragmentActivity extends Activity {
	protected abstract Fragment createFragment();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fragment);
		FragmentManager manager = getFragmentManager();
		if (manager.findFragmentById(R.id.fragmentContainer) == null)
			manager.beginTransaction().add(R.id.fragmentContainer, createFragment()).commit();
	}
}
