package com.android.isoma.tasks;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import nl.siegmann.epublib.domain.Book;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.htmlcleaner.TagNode;

import com.android.isoma.enc.ProcessData;
import com.android.isoma.epub.PageTurnerSpine;
import com.android.isoma.view.BookView;
import com.isoma.htmlspanner.HtmlSpanner;
import com.isoma.htmlspanner.TagNodeHandler;
import com.isoma.htmlspanner.handlers.TableHandler;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

public class SearchTextTask
		extends
		AsyncTask<String, SearchTextTask.SearchResult, SearchTextTask.SearchResult> {

	private Book book;

	private HtmlSpanner spanner;
	private BookView bookView;
	private String searchValue;
	private SearchResult previousResult;
	private String fileName;
	private Context context;

	public SearchTextTask(Book book, SearchResult previousResult, String fileName, Context context) {
		this.book = book;
		this.previousResult = previousResult;
        this.fileName=fileName;
		this.spanner = new HtmlSpanner();
        this.context=context;
		DummyHandler dummy = new DummyHandler();

		spanner.registerHandler("img", dummy);
		spanner.registerHandler("image", dummy);

		spanner.registerHandler("table", new TableHandler());
		
	}

	@Override
	protected SearchResult doInBackground(String... params) {
		SearchResult res = new SearchResult(null, 0, 0, 0, 0);
		String searchTerm = params[0];
		Pattern pattern = Pattern.compile(Pattern.quote((searchTerm.trim())),
				Pattern.CASE_INSENSITIVE);

		try {

			PageTurnerSpine spine = new PageTurnerSpine(book);

			Boolean found = false;
			
			int lastIndex = 0;
			int lastEndPoint = 0;
			if (found == false) {
				if (previousResult != null){
					
					String lastSearchterm = previousResult.getDisplay();
					
					if(lastSearchterm.equalsIgnoreCase(searchTerm)){
						
						lastIndex = previousResult.getIndex();
						lastEndPoint = previousResult.getEnd();	
					}
				
				}
				
				for (int index = lastIndex; ((index < spine.size()) && (found == false)); index++) {

					spine.navigateByIndex(index);

					int progress = spine.getProgressPercentage(index, 0);
					publishProgress(new SearchResult(null, index, 0, 0,
							progress));

						Spannable spaned = null;
					
					
					int period = fileName.lastIndexOf(".");
					String check = fileName.substring(period - 4, period);

					if (check.equalsIgnoreCase("_ENC")) {
						ProcessData process = new ProcessData();

						try {
							spaned = (Spannable) spanner.fromHtml(process.decryption(spine.getCurrentResource().getData(), context));
						} catch (DataLengthException e) {
							e.printStackTrace();
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (InvalidCipherTextException e) {
							e.printStackTrace();
						}

					}else{
						spaned = (Spannable) spanner.fromHtml(spine.getCurrentResource().getReader());
					}
				
					Matcher matcher = pattern.matcher(spaned);
					
					if ((matcher.find(lastEndPoint) == true)) {
						int from = Math.max(0, matcher.start());
						int to = Math.min(spaned.length() - 1,
								matcher.end());
						
						if (isCancelled()) {
							return null;
						}
					
						String text = spaned.subSequence(from, to).toString().trim();
						
							res = new SearchResult(text, index,
									matcher.start(), matcher.end(),
									spine.getProgressPercentage(index,
											matcher.start()));
						
						this.publishProgress(res);
						found = true;
						
						return res;
					} else {
						found = false;
					}
				}
			}
		} catch (IOException io) {
			return null;
		}

		return res;
	}

	public static class SearchResult {

		private String display;
		private int index;
		private int start;
		private int end;

		private int percentage;

		public SearchResult(String display, int index, int offset, int end,
				int percentage) {
			this.display = display;
			this.index = index;
			this.start = offset;
			this.end = end;
			this.percentage = percentage;
		}

		public String getDisplay() {
			return display;
		}

		public int getIndex() {
			return index;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public int getPercentage() {
			return percentage;
		}
	}

	private static class DummyHandler extends TagNodeHandler {
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {

			builder.append("\uFFFC");
		}
	}
}
