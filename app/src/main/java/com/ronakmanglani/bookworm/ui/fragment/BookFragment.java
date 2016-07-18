package com.ronakmanglani.bookworm.ui.fragment;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.ronakmanglani.bookworm.BookWormApp;
import com.ronakmanglani.bookworm.R;
import com.ronakmanglani.bookworm.data.BookColumns;
import com.ronakmanglani.bookworm.data.BookProvider;
import com.ronakmanglani.bookworm.model.Book;
import com.ronakmanglani.bookworm.util.DimenUtil;
import com.ronakmanglani.bookworm.api.VolleySingleton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.support.v7.widget.Toolbar.*;

public class BookFragment extends Fragment implements OnMenuItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int SHELF_LOADER = 42;

    private Book book;
    private int currentShelf;
    private Unbinder unbinder;

    @BindView(R.id.toolbar)                 Toolbar toolbar;
    @BindView(R.id.toolbar_text_holder)     View toolbarTextHolder;
    @BindView(R.id.toolbar_title)           TextView toolbarTitle;
    @BindView(R.id.toolbar_subtitle)        TextView toolbarSubtitle;

    @BindView(R.id.book_message_holder)     View bookMessageHolder;
    @BindView(R.id.book_detail_holder)      NestedScrollView bookDetailHolder;

    @BindView(R.id.book_cover)              NetworkImageView bookCover;
    @BindView(R.id.book_title)              TextView bookTitle;
    @BindView(R.id.book_author)             TextView bookAuthors;
    @BindView(R.id.book_page)               TextView bookPageCount;
    @BindView(R.id.book_rating_holder)      View bookRatingHolder;
    @BindView(R.id.book_rating)             TextView bookRating;
    @BindView(R.id.book_vote_count)         TextView bookVoteCount;

    @BindView(R.id.book_ranking_holder)     View bookRankingHolder;
    @BindView(R.id.book_current_rank)       TextView bookCurrentRank;
    @BindView(R.id.book_weeks_list)         TextView bookWeeksOnList;

    @BindView(R.id.book_publication_holder) View bookPublisherHolder;
    @BindView(R.id.book_publication_name)   TextView bookPublisher;
    @BindView(R.id.book_publication_date)   TextView bookDate;
    @BindView(R.id.book_publication_isbn)   TextView bookIsbn;

    @BindView(R.id.book_description_holder) View bookDescriptionHolder;
    @BindView(R.id.book_description)        TextView bookDescription;

    @BindView(R.id.fab_menu)                FloatingActionMenu fabMenu;
    @BindView(R.id.fab_to_read)             FloatingActionButton fabToRead;
    @BindView(R.id.fab_reading)             FloatingActionButton fabReading;
    @BindView(R.id.fab_finished)            FloatingActionButton fabFinished;

    // Fragment lifecycle
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book, container, false);
        unbinder = ButterKnife.bind(this, v);

        // Get the book
        book = getArguments().getParcelable(BookWormApp.KEY_BOOK);
        if (book == null) {
            fabMenu.setVisibility(GONE);
            bookDetailHolder.setVisibility(GONE);
            if (getArguments().getBoolean(BookWormApp.KEY_VISIBILITY, false)) {
                bookMessageHolder.setVisibility(VISIBLE);
            }
            return v;
        }

        // Floating Action Buttons
        getLoaderManager().restartLoader(SHELF_LOADER, null, this);
        bookDetailHolder.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (oldScrollY < scrollY) {
                    fabMenu.hideMenuButton(true);
                } else {
                    fabMenu.showMenuButton(true);
                }
            }
        });

        // Setup toolbar
        if (book.getSubtitle().length() == 0) {
            toolbarTextHolder.setVisibility(GONE);
            toolbar.setTitle(book.getTitle());
        } else {
            toolbarTitle.setText(book.getTitle());
            toolbarSubtitle.setText(book.getSubtitle());
        }
        toolbar.setOnMenuItemClickListener(this);
        toolbar.inflateMenu(R.menu.menu_book);
        if (!DimenUtil.isTablet()) {
            toolbar.setNavigationIcon(ContextCompat.getDrawable(getActivity(), R.drawable.action_home));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Cover Image
        if (book.getImageUrl().length() == 0) {
            bookCover.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.default_cover));
        } else {
            bookCover.setImageUrl(book.getImageUrl(), VolleySingleton.getInstance().imageLoader);
        }

        // Title, author and page count
        bookTitle.setText(book.getTitle());
        if (book.getAuthors().length() == 0) {
            bookAuthors.setVisibility(GONE);
        } else {
            bookAuthors.setText(getString(R.string.detail_subtitle_by, book.getAuthors()));
        }
        if (book.getPageCount().length() == 0) {
            bookPageCount.setVisibility(GONE);
        } else {
            bookPageCount.setText(getString(R.string.detail_subtitle_page, book.getPageCount()));
        }

        // Rating
        if (book.getAverageRating().length() == 0 || book.getRatingCount().length() == 0) {
            bookRatingHolder.setVisibility(View.GONE);
        } else {
            bookRating.setText(book.getAverageRating());
            bookVoteCount.setText(getString(R.string.detail_rating, book.getRatingCount()));
        }

        // Ranking
        if (book.getCurrentRank().length() == 0 || book.getWeeksOnList().length() == 0) {
            bookRankingHolder.setVisibility(GONE);
        } else {
            bookCurrentRank.setText(getString(R.string.detail_ranking_current, book.getCurrentRank()));
            if (book.getWeeksOnList().equals("0")) {
                bookWeeksOnList.setVisibility(GONE);
            } else {
                bookWeeksOnList.setText(getString(R.string.detail_ranking_weeks, book.getWeeksOnList()));
            }
        }

        // Publication info
        String publisher = book.getPublisher();
        String publishDate = book.getPublishDate();
        String identifiers = book.getIdentifiers();
        if (publisher.length() == 0 && publishDate.length() == 0 && identifiers.length() == 0) {
            bookPublisherHolder.setVisibility(View.GONE);
        } else {
            // Publisher
            if (publisher.length() == 0) {
                bookPublisher.setVisibility(GONE);
            } else {
                bookPublisher.setText(getString(R.string.detail_publication_name, publisher));
            }
            // Publish date
            if (publishDate.length() == 0) {
                bookDate.setVisibility(GONE);
            } else {
                bookDate.setText(getString(R.string.detail_publication_date, publishDate));
            }
            // Identifiers
            if (identifiers.length() == 0) {
                bookIsbn.setVisibility(GONE);
            } else {
                bookIsbn.setText(getString(R.string.detail_publication_isbn, identifiers));
            }
        }

        // Book description
        if (book.getDescription().length() == 0) {
            bookDescriptionHolder.setVisibility(View.GONE);
        } else {
            bookDescription.setText(book.getDescription());
        }

        return v;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    // Database functions
    private ContentValues getContentValues(int shelf) {
        ContentValues values = new ContentValues();
        values.put(BookColumns.BOOK_ID, book.getUniqueId());
        values.put(BookColumns.ISBN_10, book.getIsbn10());
        values.put(BookColumns.ISBN_13, book.getIsbn13());
        values.put(BookColumns.TITLE, book.getTitle());
        values.put(BookColumns.SUBTITLE, book.getSubtitle());
        values.put(BookColumns.AUTHORS, book.getAuthors());
        values.put(BookColumns.PAGE_COUNT, book.getPageCount());
        values.put(BookColumns.AVG_RATING, book.getAverageRating());
        values.put(BookColumns.RATING_COUNT, book.getRatingCount());
        values.put(BookColumns.IMAGE_URL, book.getImageUrl());
        values.put(BookColumns.PUBLISHER, book.getPublisher());
        values.put(BookColumns.PUBLISH_DATE, book.getPublishDate());
        values.put(BookColumns.DESCRIPTION, book.getDescription());
        values.put(BookColumns.ITEM_URL, book.getItemUrl());
        values.put(BookColumns.SHELF, shelf);
        return values;
    }
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case SHELF_LOADER:
                return new CursorLoader(
                        getActivity(),                                              // Parent activity context
                        BookProvider.Books.CONTENT_URI,                             // Table to query
                        new String[] { BookColumns.SHELF },                         // Projection to return
                        BookColumns.BOOK_ID + " = '" + book.getUniqueId() + "'",    // Selection clause
                        null,                                                       // No selection arguments
                        null);                                                      // Default sort order

            default:
                return null;
        }
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() > 0) {
            // If book already exists in database
            data.moveToFirst();
            currentShelf = data.getInt(data.getColumnIndex(BookColumns.SHELF));
            switch (currentShelf) {

                case BookColumns.SHELF_TO_READ:
                    fabToRead.setVisibility(VISIBLE);
                    fabReading.setVisibility(VISIBLE);
                    fabFinished.setVisibility(VISIBLE);
                    fabToRead.setLabelText(getString(R.string.detail_fab_to_read_remove));
                    fabReading.setLabelText(getString(R.string.detail_fab_reading));
                    fabFinished.setLabelText(getString(R.string.detail_fab_finished));
                    break;

                case BookColumns.SHELF_READING:
                    fabToRead.setVisibility(GONE);
                    fabReading.setVisibility(VISIBLE);
                    fabFinished.setVisibility(VISIBLE);
                    fabReading.setLabelText(getString(R.string.detail_fab_reading_remove));
                    fabFinished.setLabelText(getString(R.string.detail_fab_finished));
                    break;

                case BookColumns.SHELF_FINISHED:
                    fabToRead.setVisibility(GONE);
                    fabReading.setVisibility(GONE);
                    fabFinished.setVisibility(VISIBLE);
                    fabFinished.setLabelText(getString(R.string.detail_fab_finished_remove));
                    break;

            }
            data.close();   // Close query cursor
        } else {
            // Book not in database
            currentShelf = BookColumns.SHELF_NONE;
            fabToRead.setVisibility(VISIBLE);
            fabReading.setVisibility(VISIBLE);
            fabFinished.setVisibility(VISIBLE);
            fabToRead.setLabelText(getString(R.string.detail_fab_to_read));
            fabReading.setLabelText(getString(R.string.detail_fab_reading));
            fabFinished.setLabelText(getString(R.string.detail_fab_finished));
        }
    }
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    // Click events
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            case R.id.action_share:
                String shareSubject = getString(R.string.action_share_subject, book.getTitle());
                String shareText = getString(R.string.action_share_text, book.getTitle(), book.getAuthors(), book.getItemUrl());
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.action_share_title)));
                return true;

            case R.id.action_open:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(book.getItemUrl()));
                startActivity(browserIntent);
                return true;

            default:
                return false;
        }
    }
    @OnClick(R.id.fab_to_read)
    public void onToReadButtonClicked() {
        if (currentShelf == BookColumns.SHELF_TO_READ) {
            // Remove from "To Read"
            getContext().getContentResolver().
                    delete(BookProvider.Books.CONTENT_URI,
                            BookColumns.BOOK_ID + " = '" + book.getUniqueId() + "'",
                            null);
        } else {
            // Insert into "To Read"
            getContext().getContentResolver().
                    insert(BookProvider.Books.CONTENT_URI,
                            getContentValues(BookColumns.SHELF_TO_READ));
        }
        getLoaderManager().restartLoader(SHELF_LOADER, null, this);
    }
    @OnClick(R.id.fab_reading)
    public void onReadingButtonClicked() {
        if (currentShelf == BookColumns.SHELF_TO_READ) {
            // Move from "To Read" to "Reading"
            ContentValues values = new ContentValues();
            values.put(BookColumns.SHELF, BookColumns.SHELF_READING);
            getContext().getContentResolver().
                    update(BookProvider.Books.CONTENT_URI, values,
                            BookColumns.BOOK_ID + " = '" + book.getUniqueId() + "'",
                            new String[]{});
        } else if (currentShelf == BookColumns.SHELF_READING) {
            // Remove from "Reading"
            getContext().getContentResolver().
                    delete(BookProvider.Books.CONTENT_URI,
                            BookColumns.BOOK_ID + " = '" + book.getUniqueId() + "'",
                            null);
        } else {
            // Insert into "Reading"
            getContext().getContentResolver().
                    insert(BookProvider.Books.CONTENT_URI,
                            getContentValues(BookColumns.SHELF_READING));
        }
        getLoaderManager().restartLoader(SHELF_LOADER, null, this);
    }
    @OnClick(R.id.fab_finished)
    public void onFinishedButtonClicked() {
        if (currentShelf == BookColumns.SHELF_TO_READ || currentShelf == BookColumns.SHELF_READING) {
            // Move from "To Read" or "Reading" to "Finished"
            ContentValues values = new ContentValues();
            values.put(BookColumns.SHELF, BookColumns.SHELF_FINISHED);
            getContext().getContentResolver().
                    update(BookProvider.Books.CONTENT_URI, values,
                            BookColumns.BOOK_ID + " = '" + book.getUniqueId() + "'",
                            new String[]{});
        } else if (currentShelf == BookColumns.SHELF_FINISHED) {
            // Remove from "Finished"
            getContext().getContentResolver().
                    delete(BookProvider.Books.CONTENT_URI,
                            BookColumns.BOOK_ID + " = '" + book.getUniqueId() + "'",
                            null);
        } else {
            // Insert into "Finished"
            getContext().getContentResolver().
                    insert(BookProvider.Books.CONTENT_URI,
                            getContentValues(BookColumns.SHELF_FINISHED));
        }
        getLoaderManager().restartLoader(SHELF_LOADER, null, this);
    }
}
