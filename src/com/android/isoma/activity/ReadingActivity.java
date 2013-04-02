/*
 * Copyright (C) 2011 Alex Kuiper
 * 
 * This file is part of Isoma
 *
 * Isoma is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Isoma is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Isoma.  If not, see <http://www.gnu.org/licenses/>.*
 */

package com.android.isoma.activity;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import nl.siegmann.epublib.domain.Book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.isoma.R;
import com.android.isoma.animation.Animations;
import com.android.isoma.animation.Animator;
import com.android.isoma.animation.PageCurlAnimator;
import com.android.isoma.animation.PageTimer;
import com.android.isoma.animation.RollingBlindAnimator;
import com.android.isoma.library.LibraryService;
import com.android.isoma.sync.BookProgress;
import com.android.isoma.sync.ProgressService;
import com.android.isoma.tasks.SearchTextTask;
import com.android.isoma.tasks.SearchTextTask.SearchResult;
import com.android.isoma.view.AnimatedImageView;
import com.android.isoma.view.BookView;
import com.android.isoma.view.BookViewListener;
import com.global.android.widget.VerifiedFlingListener;
import com.google.inject.Inject;
import com.isoma.htmlspanner.HtmlSpanner;

public class ReadingActivity extends RoboActivity implements BookViewListener {

	private static final String POS_KEY = "offset:";
	private static final String IDX_KEY = "index:";

	protected static final int REQUEST_CODE_GET_CONTENT = 2;

	public static final String PICK_RESULT_ACTION = "colordict.intent.action.PICK_RESULT";

	public static final String SEARCH_ACTION = "colordict.intent.action.SEARCH";
	public static final String EXTRA_QUERY = "EXTRA_QUERY";
	public static final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";
	public static final String EXTRA_HEIGHT = "EXTRA_HEIGHT";
	public static final String EXTRA_WIDTH = "EXTRA_WIDTH";
	public static final String EXTRA_GRAVITY = "EXTRA_GRAVITY";
	public static final String EXTRA_MARGIN_LEFT = "EXTRA_MARGIN_LEFT";
	public static final String EXTRA_MARGIN_TOP = "EXTRA_MARGIN_TOP";
	public static final String EXTRA_MARGIN_BOTTOM = "EXTRA_MARGIN_BOTTOM";
	public static final String EXTRA_MARGIN_RIGHT = "EXTRA_MARGIN_RIGHT";

	private static final Logger LOG = LoggerFactory
			.getLogger(ReadingActivity.class);

	@Inject
	private ProgressService progressService;

	@Inject
	private LibraryService libraryService;

	@InjectView(R.id.mainContainer)
	private ViewSwitcher viewSwitcher;

	@InjectView(R.id.bookView)
	private BookView bookView;

	@InjectView(R.id.myTitleBarTextView)
	private TextView titleBar;

	@InjectView(R.id.myTitleBarLayout)
	private LinearLayout titleBarLayout;

	@InjectView(R.id.dummyView)
	private AnimatedImageView dummyView;

	private ProgressDialog waitDialog;
	private ProgressDialog searchDialog;
	private AlertDialog tocDialog;

	private SharedPreferences settings;

	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;

	private String bookTitle;
	private String titleBase;
	private String colourProfile;
	private String fileName;
	private int progressPercentage;

	private boolean oldBrightness = false;
	private boolean oldStripWhiteSpace = false;

	private enum Orientation {
		HORIZONTAL, VERTICAL
	}

	private CharSequence selectedWord = null;

	private Handler handler;
	private SearchResult previousResult;

	private EditText input;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Restore preferences
		this.settings = PreferenceManager.getDefaultSharedPreferences(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.read_book);

		this.handler = new Handler();

		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);

		this.gestureDetector = new GestureDetector(new SwipeListener());
		this.gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};

		this.viewSwitcher.setOnTouchListener(gestureListener);
		this.bookView.setOnTouchListener(gestureListener);
		this.bookView.addListener(this);
		this.bookView.setSpanner(getInjector().getInstance(HtmlSpanner.class));

		this.oldBrightness = settings.getBoolean("set_brightness", false);
		this.oldStripWhiteSpace = settings
				.getBoolean("strip_whitespace", false);

		registerForContextMenu(bookView);

		String file = getIntent().getStringExtra("file_name");

		if (file == null && getIntent().getData() != null) {
			file = getIntent().getData().getPath();
		}

		if (file == null) {
			file = settings.getString("last_file", "");
		}

		updateFromPrefs();
		updateFileName(savedInstanceState, file);

		if ("".equals(fileName)) {
			Log.i("isoma", "You are reading" + fileName);

			Intent intent = new Intent(this, LibraryActivity.class);
			startActivity(intent);
			finish();
			return;

		} else {
			String email = settings.getString("email", "").trim();

			if (savedInstanceState == null && email.length() != 0) {
				new DownloadProgressTask().execute();
			} else {
				bookView.restore();
			}
		}
	}

	private void updateFileName(Bundle savedInstanceState, String fileName) {

		this.fileName = fileName;

		int lastPos = -1;
		int lastIndex = 0;

		if (settings != null) {
			lastPos = settings.getInt(POS_KEY + fileName, -1);
			lastIndex = settings.getInt(IDX_KEY + fileName, -1);
		}

		if (savedInstanceState != null) {
			lastPos = savedInstanceState.getInt(POS_KEY, -1);
			lastIndex = savedInstanceState.getInt(IDX_KEY, -1);
		}

		this.bookView.setFileName(fileName);
		this.bookView.setPosition(lastPos);
		this.bookView.setIndex(lastIndex);

		// Slightly hacky
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("last_file", fileName);
		editor.commit();

	}

	public void progressUpdate(int progressPercentage) {
		if (titleBase == null) {
			return;
		}

		this.progressPercentage = progressPercentage;

		String title = this.titleBase;

		SpannableStringBuilder spannedTitle = new SpannableStringBuilder();
		spannedTitle.append(title);
		spannedTitle.append(" " + progressPercentage + "%");

		this.titleBar.setTextColor(Color.WHITE);
		this.titleBar.setText(spannedTitle);
	}

	private void updateFromPrefs() {

		this.progressService.setEmail(settings.getString("email", ""));
		this.progressService.setDeviceName(settings.getString("device_name",
				Build.MODEL));

		int userTextSize = settings.getInt("itext_size", 16);
		bookView.setTextSize(userTextSize);

		int marginH = settings.getInt("margin_h", 15);
		int marginV = settings.getInt("margin_v", 15);

		updateTypeFace();

		bookView.setHorizontalMargin(marginH);
		bookView.setVerticalMargin(marginV);

		if (!isAnimating()) {
			bookView.setEnableScrolling(settings.getBoolean("scrolling", false));
		}

		bookView.setStripWhiteSpace(settings.getBoolean("strip_whitespace",
				true));

		int lineSpacing = settings.getInt("line_spacing", 0);
		bookView.setLineSpacing(lineSpacing);

		if (settings.getBoolean("full_screen", false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			this.titleBarLayout.setVisibility(View.GONE);
		} else {
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			this.titleBarLayout.setVisibility(View.VISIBLE);
		}

		restoreColorProfile();

		// Check if we need a restart
		if (settings.getBoolean("set_brightness", false) != oldBrightness
				|| settings.getBoolean("strip_whitespace", false) != oldStripWhiteSpace) {
			Intent intent = new Intent(this, ReadingActivity.class);
			intent.setData(Uri.parse(this.fileName));
			startActivity(intent);
			finish();
		}

		String orientation = settings
				.getString("screen_orientation", "no_lock");

		if ("portrait".equals(orientation)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if ("landscape".endsWith(orientation)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}

	}

	private void updateTypeFace() {
		String fontFace = settings.getString("font_face", "sans");

		Typeface face = Typeface.SANS_SERIF;

		if ("helvet_n_rom".equals(fontFace)) {
			face = Typeface.createFromAsset(getAssets(), "helvet_n_rom.otf");
		} else if ("sans".equals(fontFace)) {
			face = Typeface.SANS_SERIF;
		} else if ("mono".equals(fontFace)) {
			face = Typeface.MONOSPACE;
		}

		this.bookView.setTypeface(face);

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			updateFromPrefs();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return bookView.onTouchEvent(event);
	}

	public void bookOpened(Book book) {

		this.bookTitle = book.getTitle();
		this.titleBase = this.bookTitle;
		setTitle(titleBase);

		new AddBookToLibraryTask().execute(book);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		// This is a hack to give the longclick handler time
		// to find the word the user long clicked on.

		if (this.selectedWord != null) {

			final CharSequence word = this.selectedWord;

			String header = String.format(getString(R.string.word_select),
					selectedWord);
			menu.setHeaderTitle(header);

			final Intent intent = new Intent(PICK_RESULT_ACTION);
			intent.putExtra(EXTRA_QUERY, word.toString()); // Search Query
			intent.putExtra(EXTRA_FULLSCREEN, false); //
			intent.putExtra(EXTRA_HEIGHT, 400); // 400pixel, if you don't
												// specify, fill_parent"
			intent.putExtra(EXTRA_GRAVITY, Gravity.BOTTOM);
			intent.putExtra(EXTRA_MARGIN_LEFT, 100);

			if (isIntentAvailable(this, intent)) {
				MenuItem item = menu.add(getString(R.string.dictionary_lookup));
				item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					public boolean onMenuItemClick(MenuItem item) {
						startActivityForResult(intent, 5);
						return true;
					}
				});
			}

			MenuItem newItem = menu.add(getString(R.string.wikipedia_lookup));
			newItem.setOnMenuItemClickListener(new BrowserSearchMenuItem(
					"http://en.wikipedia.org/wiki/Special:Search?search="
							+ URLEncoder.encode(word.toString())));

			MenuItem newItem2 = menu.add(getString(R.string.google_lookup));
			newItem2.setOnMenuItemClickListener(new BrowserSearchMenuItem(
					"http://www.google.com/search?q="
							+ URLEncoder.encode(word.toString())));

			this.selectedWord = null;
		}
	}

	private class BrowserSearchMenuItem implements OnMenuItemClickListener {

		private String launchURL;

		public BrowserSearchMenuItem(String url) {
			this.launchURL = url;
		}

		public boolean onMenuItemClick(MenuItem item) {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(this.launchURL));
			startActivity(i);

			return true;
		}
	}

	public static boolean isIntentAvailable(Context context, Intent intent) {
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private void restoreColorProfile() {

		int brightness = 50;

		if (settings.getBoolean("night_mode", false)) {
			this.colourProfile = "night";
			brightness = settings.getInt("night_bright", 50);
		} else {
			this.colourProfile = "day";
			brightness = settings.getInt("day_bright", 50);
		}

		this.bookView.setBackgroundColor(getBackgroundColor());
		this.viewSwitcher.setBackgroundColor(getBackgroundColor());
		this.bookView.setTextColor(getTextColor());
		this.bookView.setLinkColor(getLinkColor());

		if (settings.getBoolean("set_brightness", false)) {
			WindowManager.LayoutParams lp = getWindow().getAttributes();
			lp.screenBrightness = (float) brightness / 100f;
			getWindow().setAttributes(lp);
		}
	}

	private int getBackgroundColor() {
		if ("night".equals(this.colourProfile)) {
			return settings.getInt("night_bg", Color.BLACK);
		} else {
			return settings.getInt("day_bg", Color.WHITE);
		}
	}

	private int getTextColor() {
		if ("night".equals(this.colourProfile)) {
			return settings.getInt("night_text", Color.GRAY);
		} else {
			return settings.getInt("day_text", Color.BLACK);
		}
	}

	private int getLinkColor() {
		if ("night".equals(this.colourProfile)) {
			return settings.getInt("night_link", Color.rgb(255, 165, 0));
		} else {
			return settings.getInt("day_link", Color.BLUE);
		}
	}

	public void errorOnBookOpening(String errorMessage) {
		this.waitDialog.hide();
		String message = String.format(getString(R.string.error_open_bk),
				errorMessage);
		bookView.setText(new SpannedString(message));
	}

	public void parseEntryComplete(int entry, String name) {
		if (name != null && !name.equals(this.bookTitle)) {
			this.titleBase = this.bookTitle + " - " + name;
		} else {
			this.titleBase = this.bookTitle;
		}

		setTitle(this.titleBase);
		this.waitDialog.hide();
	}

	public void parseEntryStart(int entry) {
		this.viewSwitcher.clearAnimation();
		this.viewSwitcher.setBackgroundDrawable(null);
		restoreColorProfile();

		this.waitDialog.setTitle(getString(R.string.loading_wait));
		this.waitDialog.show();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();

		if (isAnimating() && action == KeyEvent.ACTION_DOWN) {
			stopAnimating();
			return true;
		}

		switch (keyCode) {

		case KeyEvent.KEYCODE_VOLUME_DOWN:
			// Yes, this is nasty: if the setting is true, we fall through to
			// the next case.
			if (!settings.getBoolean("nav_vol", false)) {
				return false;
			}

		case KeyEvent.KEYCODE_DPAD_RIGHT:

			if (action == KeyEvent.ACTION_DOWN) {
				pageDown(Orientation.HORIZONTAL);
			}

			return true;

		case KeyEvent.KEYCODE_VOLUME_UP:
			// Same dirty trick.
			if (!settings.getBoolean("nav_vol", false)) {
				return false;
			}

		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (action == KeyEvent.ACTION_DOWN) {
				pageUp(Orientation.HORIZONTAL);
			}

			return true;

		case KeyEvent.KEYCODE_BACK:
			if (action == KeyEvent.ACTION_DOWN && bookView.hasPrevPosition()) {
				bookView.goBackInHistory();

				return true;
			} else {
				this.finish();
			}

		}

		return false;
	}

	private boolean isAnimating() {
		Animator anim = dummyView.getAnimator();
		return anim != null && !anim.isFinished();
	}

	private void startAutoScroll() {

		if (viewSwitcher.getCurrentView() == this.dummyView) {
			viewSwitcher.showNext();
		}

		this.viewSwitcher.setInAnimation(null);
		this.viewSwitcher.setOutAnimation(null);

		bookView.setKeepScreenOn(true);

		String style = settings.getString("scroll_style", "rolling_blind");

		if ("rolling_blind".equals(style)) {
			prepareRollingBlind();
		} else {
			preparePageTimer();
		}

		viewSwitcher.showNext();

		handler.post(new AutoScrollRunnable());
	}

	private void prepareRollingBlind() {

		Bitmap before = getBookViewSnapshot();

		bookView.pageDown();
		Bitmap after = getBookViewSnapshot();

		RollingBlindAnimator anim = new RollingBlindAnimator();
		anim.setAnimationSpeed(settings.getInt("scroll_speed", 20));

		anim.setBackgroundBitmap(before);
		anim.setForegroundBitmap(after);

		dummyView.setAnimator(anim);
	}

	private void preparePageTimer() {
		bookView.pageDown();
		Bitmap after = getBookViewSnapshot();

		PageTimer timer = new PageTimer();
		timer.setBackgroundImage(after);
		timer.setSpeed(settings.getInt("scroll_speed", 20));

		dummyView.setAnimator(timer);
	}

	private void doPageCurl(boolean flipRight) {

		if (isAnimating() || bookView == null) {
			return;
		}

		this.viewSwitcher.setInAnimation(null);
		this.viewSwitcher.setOutAnimation(null);

		if (viewSwitcher.getCurrentView() == this.dummyView) {
			viewSwitcher.showNext();
		}

		Bitmap before = getBookViewSnapshot();

		// this.pageNumberView.setVisibility(View.GONE);

		PageCurlAnimator animator = new PageCurlAnimator(flipRight);

		// Pagecurls should only take a few frames. When the screen gets
		// bigger, so do the frames.
		animator.SetCurlSpeed(bookView.getWidth() / 8);

		animator.setBackgroundColor(getBackgroundColor());
		// animator.setEdgeColor(settings.getInt("day_text", Color.BLACK));

		if (flipRight) {
			bookView.pageDown();
			Bitmap after = getBookViewSnapshot();

			animator.setBackgroundBitmap(after);
			animator.setForegroundBitmap(before);
		} else {
			bookView.pageUp();
			Bitmap after = getBookViewSnapshot();

			animator.setBackgroundBitmap(before);
			animator.setForegroundBitmap(after);
		}

		dummyView.setAnimator(animator);

		this.viewSwitcher.showNext();

		handler.post(new PageCurlRunnable(animator));

		dummyView.invalidate();

	}

	private class PageCurlRunnable implements Runnable {

		private PageCurlAnimator animator;

		public PageCurlRunnable(PageCurlAnimator animator) {
			this.animator = animator;
		}

		public void run() {

			if (this.animator.isFinished()) {

				if (viewSwitcher.getCurrentView() == dummyView) {
					viewSwitcher.showNext();
				}

				dummyView.setAnimator(null);

			} else {
				this.animator.advanceOneFrame();
				dummyView.invalidate();

				int delay = 1000 / this.animator.getAnimationSpeed();

				handler.postDelayed(this, delay);
			}
		}

	}

	private class AutoScrollRunnable implements Runnable {
		public void run() {

			if (dummyView.getAnimator() == null) {
				LOG.debug("BookView no longer has an animator. Aborting rolling blind.");
				stopAnimating();
			} else {

				Animator anim = dummyView.getAnimator();

				if (anim.isFinished()) {
					startAutoScroll();
				} else {
					anim.advanceOneFrame();
					dummyView.invalidate();

					handler.postDelayed(this, anim.getAnimationSpeed() * 2);
				}
			}
		}
	}

	private void stopAnimating() {

		if (dummyView.getAnimator() != null) {
			dummyView.getAnimator().isFinished();
			this.dummyView.setAnimator(null);
		}

		if (viewSwitcher.getCurrentView() == this.dummyView) {
			viewSwitcher.showNext();
		}

		bookView.setKeepScreenOn(false);

	}

	private Bitmap getBookViewSnapshot() {

		bookView.layout(0, 0, viewSwitcher.getWidth(), viewSwitcher.getHeight());

		try {
			bookView.buildDrawingCache(false);
			Bitmap drawingCache = bookView.getDrawingCache();

			if (drawingCache != null) {
				Bitmap copy = drawingCache
						.copy(drawingCache.getConfig(), false);
				bookView.destroyDrawingCache();
				return copy;
			}

		} catch (OutOfMemoryError out) {
			restoreBackgroundColour();
		}

		return null;
	}

	private void prepareSlide(Animation inAnim, Animation outAnim) {

		dummyView.setVisibility(View.VISIBLE);

		Bitmap bitmap = getBookViewSnapshot();

		if (bitmap != null) {
			dummyView.setImageBitmap(bitmap);
		}

		viewSwitcher.layout(0, 0, viewSwitcher.getWidth(),
				viewSwitcher.getHeight());
		this.viewSwitcher.showNext();

		this.viewSwitcher.setInAnimation(inAnim);
		this.viewSwitcher.setOutAnimation(outAnim);
	}

	private void restoreBackgroundColour() {
		if (this.colourProfile.equals("day")) {
			this.viewSwitcher.setBackgroundColor(settings.getInt("day_bg",
					Color.WHITE));
		} else {
			this.viewSwitcher.setBackgroundColor(settings.getInt("night_bg",
					Color.BLACK));
		}
	}

	private void pageDown(Orientation o) {

		stopAnimating();

		String animH = settings.getString("h_animation", "curl");
		String animV = settings.getString("v_animation", "slide");

		if (o == Orientation.HORIZONTAL) {

			if ("curl".equals(animH)) {
				doPageCurl(true);
			} else if ("slide".equals(animH)) {
				prepareSlide(Animations.inFromRightAnimation(),
						Animations.outToLeftAnimation());
				this.viewSwitcher.showNext();
				bookView.pageDown();
			} else {
				bookView.pageDown();
			}

		} else if ("slide".equals(animV)) {
			prepareSlide(Animations.inFromBottomAnimation(),
					Animations.outToTopAnimation());
			this.viewSwitcher.showNext();
			bookView.pageDown();
		}

	}

	private void pageUp(Orientation o) {

		stopAnimating();

		String animH = settings.getString("h_animation", "curl");
		String animV = settings.getString("v_animation", "slide");

		if (o == Orientation.HORIZONTAL) {

			if ("curl".equals(animH)) {
				doPageCurl(false);
			} else if ("slide".equals(animH)) {
				prepareSlide(Animations.inFromLeftAnimation(),
						Animations.outToRightAnimation());
				this.viewSwitcher.showNext();
				bookView.pageUp();
			} else {
				bookView.pageUp();
			}

		} else if ("slide".equals(animV)) {
			prepareSlide(Animations.inFromTopAnimation(),
					Animations.outToBottomAnimation());
			this.viewSwitcher.showNext();
			bookView.pageUp();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		if (this.tocDialog == null) {
			initTocDialog();
		}

		MenuItem nightMode = menu.findItem(R.id.profile_night);
		MenuItem dayMode = menu.findItem(R.id.profile_day);

		MenuItem showToc = menu.findItem(R.id.show_toc);
		// MenuItem sync = menu.findItem(R.id.manual_sync);

		showToc.setEnabled(this.tocDialog != null);
		// sync.setEnabled(!"".equals(settings.getString("email", "")));

		if (this.colourProfile.equals("day")) {
			dayMode.setVisible(false);
			nightMode.setVisible(true);
		} else {
			dayMode.setVisible(true);
			nightMode.setVisible(false);
		}

		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.titleBarLayout.setVisibility(View.VISIBLE);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		if (settings.getBoolean("full_screen", false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			this.titleBarLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * This is called after the file manager finished.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK && data != null) {
			// obtain the filename
			Uri fileUri = data.getData();
			if (fileUri != null) {
				String filePath = fileUri.getPath();
				if (filePath != null) {
					loadNewBook(filePath);
				}
			}
		}

	}

	private void loadNewBook(String fileName) {
		setTitle(R.string.app_name);
		this.tocDialog = null;
		this.bookTitle = null;
		this.titleBase = null;

		bookView.clear();

		updateFileName(null, fileName);
		new DownloadProgressTask().execute();
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (this.bookView != null) {
			progressService.storeProgress(this.fileName,
					this.bookView.getIndex(), this.bookView.getPosition(),
					this.progressPercentage);

			// We need an Editor object to make preference changes.
			// All objects are from android.context.Context
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(POS_KEY + this.fileName, this.bookView.getPosition());
			editor.putInt(IDX_KEY + this.fileName, this.bookView.getIndex());

			// Commit the edits!
			editor.commit();
		}

		this.waitDialog.dismiss();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.reading_menu, menu);

		return true;
	}

	private void setProfile(String profileName) {

		SharedPreferences.Editor editor = this.settings.edit();
		editor.putBoolean("night_mode", !this.colourProfile.equals("night"));
		editor.commit();

		this.restoreColorProfile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {

		case R.id.profile_night:
			setProfile("night");
			return true;

		case R.id.profile_day:
			setProfile("day");
			return true;

			/*
			 * case R.id.manual_sync: new ManualProgressSync().execute(); return
			 * true;
			 */

		case R.id.preferences:

			oldBrightness = settings.getBoolean("set_brightness", false);
			oldStripWhiteSpace = settings.getBoolean("strip_whitespace", false);

			Intent i = new Intent(this, PageTurnerPrefsActivity.class);
			startActivity(i);
			return true;

		case R.id.show_toc:
			this.tocDialog.show();
			return true;

			/*
			 * case R.id.open_file: launchFileManager(); return true;
			 */

		case R.id.open_library:
			launchLibrary();
			return true;

		case R.id.rolling_blind:
			startAutoScroll();
			return true;

		case R.id.about:
			// showAboutDialog();
			onSearchClick();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * This creates a search dialog that allows us to search the document text.
	 */
	private void onSearchClick() {

		final AlertDialog.Builder searchInputDialogBuilder = new AlertDialog.Builder(
				this);

		searchInputDialogBuilder.setTitle(R.string.search_text);
		searchInputDialogBuilder.setMessage(R.string.enter_query);

		final SearchTextTask task = new SearchTextTask(bookView.getBook(),
				previousResult, fileName, getApplicationContext()) {

		
			protected void onPreExecute() {
				super.onPreExecute();

			}

			protected void onProgressUpdate(SearchResult... values) {
				super.onProgressUpdate(values);
				LOG.debug("Found match at index=" + values[0].getIndex()
						+ ", offset=" + values[0].getStart() + " with context "
						+ values[0].getDisplay());
				SearchResult res = values[0];

				if (res.getDisplay() != null) {
					previousResult = res;
				}
			}

			protected void onCancelled() {
				Toast.makeText(ReadingActivity.this, R.string.search_cancelled,
						Toast.LENGTH_LONG).show();
			}

			protected void onPostExecute(SearchResult result) {

				searchDialog.dismiss();

				if (!isCancelled()) {
					if (result.getDisplay() != null) {
						// showSearchResultDialog(result);
						bookView.navigateBySearchResult(result);
						
					} else {
						Toast.makeText(ReadingActivity.this,
								R.string.search_no_matches, Toast.LENGTH_LONG)
								.show();
					}
				}
			};
		};
		input = new EditText(ReadingActivity.this);
		// Create a key value pair for the text in the search dialog EditText
		// view.
		// By default the text field should be empty
		// On creating the app, the shared preferences file called I_SEARCH will
		// be created
		SharedPreferences iSearch = getSharedPreferences("Search_Value", 0);
		input.setText(iSearch.getString("searchValue", ""));

		input.setInputType(InputType.TYPE_CLASS_TEXT);
		searchInputDialogBuilder.setView(input);

		searchDialog = ProgressDialog.show(ReadingActivity.this,
				"Isoma Search", getText(R.string.search_wait), true, true,
				new OnCancelListener() {
					/**
					 * Cancel the search task
					 */
					public void onCancel(DialogInterface dialog) {
						task.cancel(true);
					}
				});
		searchInputDialogBuilder.setPositiveButton(android.R.string.search_go,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Get a refference to the Shared preferences file
						SharedPreferences iSearch = getSharedPreferences(
								"Search_Value", 0);
						SharedPreferences.Editor isEditor = iSearch.edit();
						// Get the edit text value and place it in the shared
						// preferences
						isEditor.putString("searchValue", input.getText()
								.toString());
						// To commit the changes
						isEditor.commit();

						// After commiting the search term to the shared prefs,
						// we execute the search task.
						task.execute(input.getText().toString());
					}
				});

		searchInputDialogBuilder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						searchDialog.dismiss();
						task.cancel(true);
						Toast.makeText(ReadingActivity.this,
								R.string.search_cancelled, Toast.LENGTH_LONG)
								.show();
					}
				});

		final AlertDialog searchInputDialog = searchInputDialogBuilder.show();

		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (event == null) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						task.execute(input.getText().toString());
						searchInputDialog.dismiss();
						return true;
					}
				} else if (actionId == EditorInfo.IME_NULL) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						task.execute(input.getText().toString());
						searchInputDialog.dismiss();
					}

					return true;
				}

				return false;
			}
		});

	}






	private void launchLibrary() {
		Intent intent = new Intent(this, LibraryActivity.class);
		startActivity(intent);
	}

	

	

	private void initTocDialog() {

		if (this.tocDialog != null) {
			return;
		}

		final List<BookView.TocEntry> tocList = this.bookView
				.getTableOfContents();

		if (tocList == null || tocList.isEmpty()) {
			return;
		}

		final CharSequence[] items = new CharSequence[tocList.size()];

		for (int i = 0; i < items.length; i++) {
			items[i] = tocList.get(i).getTitle();
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.toc_label);

		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				bookView.navigateTo(tocList.get(item).getHref());
			}
		});

		this.tocDialog = builder.create();
		this.tocDialog.setOwnerActivity(this);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (this.bookView != null) {
			progressService.storeProgress(this.fileName,
					this.bookView.getIndex(), this.bookView.getPosition(),
					this.progressPercentage);

			outState.putInt(POS_KEY, this.bookView.getPosition());
			outState.putInt(IDX_KEY, this.bookView.getIndex());

			this.libraryService.close();
		}
	}

	private class AddBookToLibraryTask extends AsyncTask<Book, Integer, Void> {
		@Override
		protected Void doInBackground(Book... params) {

			Book book = params[0];
			boolean copy = settings.getBoolean("copy_to_library", true);
			try {
				libraryService.storeBook(fileName, book, true, copy);
			} catch (IOException io) {
				LOG.error("Copy to library failed.", io);
			}

			return null;
		}
	}

	

	private class DownloadProgressTask extends
			AsyncTask<Void, Integer, BookProgress> {

		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(R.string.syncing);
			waitDialog.show();
		}

		@Override
		protected BookProgress doInBackground(Void... params) {
			List<BookProgress> updates = progressService.getProgress(fileName);

			if (updates != null && updates.size() > 0) {
				return updates.get(0);
			}

			return null;
		}

		@Override
		protected void onPostExecute(BookProgress progress) {
			waitDialog.hide();

			int index = bookView.getIndex();
			int pos = bookView.getPosition();

			if (progress != null) {

				if (progress.getIndex() > index) {
					bookView.setIndex(progress.getIndex());
					bookView.setPosition(progress.getProgress());
				} else if (progress.getIndex() == index) {
					pos = Math.max(pos, progress.getProgress());
					bookView.setPosition(pos);
				}

			}

			bookView.restore();
		}
	}

	private class SwipeListener extends VerifiedFlingListener {

		public SwipeListener() {
			super(ReadingActivity.this);
		}

		public boolean onVerifiedFling(MotionEvent e1, MotionEvent e2,
				float velocityX, float velocityY) {

			stopAnimating();

			boolean swipeH = settings.getBoolean("nav_swipe_h", true);
			boolean swipeV = settings.getBoolean("nav_swipe_v", true)
					&& !settings.getBoolean("scrolling", true);

			if (swipeH && velocityX > 0) {
				pageUp(Orientation.HORIZONTAL);
				return true;
			} else if (swipeH && velocityX < 0) {
				pageDown(Orientation.HORIZONTAL);
				return true;
			} else if (swipeV && velocityY < 0) {
				pageDown(Orientation.VERTICAL);
				return true;
			} else if (swipeV && velocityY > 0) {
				pageUp(Orientation.VERTICAL);
				return true;
			}

			return false;
		}

		public boolean onSingleTapConfirmed(MotionEvent e) {

			stopAnimating();

			boolean tapH = settings.getBoolean("nav_tap_h", true);
			boolean tapV = settings.getBoolean("nav_tap_v", true);

			final int TAP_RANGE_H = bookView.getWidth() / 5;
			final int TAP_RANGE_V = bookView.getHeight() / 5;
			if (tapH) {
				if (e.getX() < TAP_RANGE_H) {
					pageUp(Orientation.HORIZONTAL);
					return true;
				} else if (e.getX() > bookView.getWidth() - TAP_RANGE_H) {
					pageDown(Orientation.HORIZONTAL);
					return true;
				}
			}

			int yBase = bookView.getScrollY();

			if (tapV) {
				if (e.getY() < TAP_RANGE_V + yBase) {
					pageUp(Orientation.VERTICAL);
					return true;
				} else if (e.getY() > (yBase + bookView.getHeight())
						- TAP_RANGE_V) {
					pageDown(Orientation.VERTICAL);
					return true;
				}
			}

			return false;
		}

		public void onLongPress(MotionEvent e) {
			CharSequence word = bookView.getWordAt(e.getX(), e.getY());
			selectedWord = word;

			openContextMenu(bookView);
		}
	}
}
