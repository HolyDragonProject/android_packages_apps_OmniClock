package org.omnirom.deskclock;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import org.omnirom.deskclock.alarms.AlarmConstants;
import org.omnirom.deskclock.provider.Alarm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrowseActivity extends Activity implements SearchView.OnQueryTextListener,
        DirectoryChooserDialog.ChosenDirectoryListener {

    public static final int QUERY_TYPE_ALARM = 0;
    public static final int QUERY_TYPE_RINGTONE = 1;
    public static final int QUERY_TYPE_RECENT = 2;
    public static final int QUERY_TYPE_ARTIST = 3;
    public static final int QUERY_TYPE_ALBUM = 4;
    public static final int QUERY_TYPE_TRACK = 5;
    public static final int QUERY_TYPE_FOLDER = 6;
    public static final int QUERY_TYPE_UNKNOWN = -1;

    private static final String PREF_RECENT_URI = "local_recent_uri";
    private static final int RECENT_SIZE = 10;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;

    private ListView mQueryList;
    private List<QueryItem> mQueryResultList = new ArrayList<QueryItem>();
    private ArrayAdapter<QueryItem> mAdapter;
    private String mSelectedUri;
    private Alarm mAlarm;
    private SearchView mSearchView;
    private int mQueryType;
    private String mCurrentQueryText = "";
    private View mCurrentUri;
    private ProgressBar mProgress;
    private View mFooterView;
    private TextView mQueryTypeText;
    private List<QueryItem> mQueryResultListCopy = new ArrayList<QueryItem>();
    private boolean mLightTheme = true;
    private ImageView mAlarmHeader;
    private ImageView mRingtoneHeader;
    private ImageView mRecentHeader;
    private ImageView mArtistHeader;
    private ImageView mAlbumHeader;
    private ImageView mTrackHeader;
    private ImageView mFolderHeader;
    private View mAlarmHeaderBar;
    private View mRingtoneHeaderBar;
    private View mRecentHeaderBar;
    private View mArtistHeaderBar;
    private View mAlbumHeaderBar;
    private View mTrackHeaderBar;
    private View mFolderHeaderBar;
    private boolean mPreAlarm;
    private List<QueryItem> mAlarms;
    private List<QueryItem> mRingtones;
    private TextView mChooseFolder;
    private boolean mHasStoragePerms;

    private class QueryItem {
        String mName;
        String mUri;
        String mSubText;
        int mQueryType;
        int mIconId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent().hasExtra(AlarmConstants.DATA_COLOR_THEME_LIGHT)) {
            mLightTheme = getIntent().getBooleanExtra(AlarmConstants.DATA_COLOR_THEME_LIGHT, true);
        } else {
            mLightTheme = Utils.isLightTheme(this);
        }
        if (mLightTheme) {
            setTheme(R.style.DeskClock);
        } else {
            setTheme(R.style.DeskClockDark);
        }
        if (getIntent().hasExtra(AlarmConstants.DATA_ALARM_EXTRA)) {
            mAlarm = getIntent().getParcelableExtra(AlarmConstants.DATA_ALARM_EXTRA);
            if (mAlarm != null) {
                mSelectedUri = mAlarm.alert.toString();
                mPreAlarm = mAlarm.preAlarm;
            }
        }
        if (mAlarm == null) {
            mAlarm = new Alarm();
            mPreAlarm = false;
        }
        if (getIntent().hasExtra(AlarmConstants.DATA_ALARM_EXTRA_URI)) {
            mSelectedUri = getIntent().getStringExtra(AlarmConstants.DATA_ALARM_EXTRA_URI);
            if (mSelectedUri != null) {
                mAlarm.alert = Uri.parse(mSelectedUri);
            }
        }

        setContentView(R.layout.browse_activity);
        mCurrentUri = findViewById(R.id.current_uri);
        mSearchView = (SearchView) findViewById(R.id.query_pattern);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setVisibility(View.INVISIBLE);
        mQueryTypeText = (TextView) findViewById(R.id.query_type_string);
        mQueryList = (ListView) findViewById(R.id.query_result);
        mFooterView = getLayoutInflater().inflate(R.layout.browse_footer_item, mQueryList, false);
        mProgress = (ProgressBar) mFooterView.findViewById(R.id.query_progressbar);
        mQueryList.addFooterView(mFooterView, null, false);
        mQueryType = QUERY_TYPE_RECENT;
        mQueryTypeText.setText(R.string.local_query_recent);
        mChooseFolder = (TextView) findViewById(R.id.query_folder_button);
        mChooseFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFolderPicker();
            }
        });

        mAdapter = createListAdapter();
        mQueryList.setAdapter(mAdapter);

        mAlarmHeader = (ImageView) findViewById(R.id.query_alarm);
        mAlarmHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchView.setVisibility(View.INVISIBLE);
                mChooseFolder.setVisibility(View.GONE);
                mQueryType = QUERY_TYPE_ALARM;
                mQueryTypeText.setText(R.string.local_query_alarm);
                clearList();
                updateTabs();
                doQuery(mCurrentQueryText, 0);
            }
        });
        mAlarmHeaderBar = findViewById(R.id.query_alarm_bar);

        mRingtoneHeader = (ImageView) findViewById(R.id.query_ringtone);
        mRingtoneHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchView.setVisibility(View.INVISIBLE);
                mChooseFolder.setVisibility(View.GONE);
                mQueryType = QUERY_TYPE_RINGTONE;
                mQueryTypeText.setText(R.string.local_query_ringtone);
                clearList();
                updateTabs();
                doQuery(mCurrentQueryText, 0);
            }
        });
        mRingtoneHeaderBar = findViewById(R.id.query_ringtone_bar);

        mRecentHeader = (ImageView) findViewById(R.id.query_recent);
        mRecentHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchView.setVisibility(View.INVISIBLE);
                mChooseFolder.setVisibility(View.GONE);
                mQueryType = QUERY_TYPE_RECENT;
                mQueryTypeText.setText(R.string.local_query_recent);
                clearList();
                updateTabs();
                doQuery(mCurrentQueryText, 0);
            }
        });
        mRecentHeaderBar = findViewById(R.id.query_recent_bar);

        mArtistHeader = (ImageView) findViewById(R.id.query_artist);
        mArtistHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchView.setVisibility(View.VISIBLE);
                mChooseFolder.setVisibility(View.GONE);
                mQueryType = QUERY_TYPE_ARTIST;
                mQueryTypeText.setText(R.string.local_query_artist);
                clearList();
                updateTabs();
                doQuery(mCurrentQueryText, 0);
            }
        });
        mArtistHeaderBar = findViewById(R.id.query_artist_bar);

        mAlbumHeader = (ImageView) findViewById(R.id.query_album);
        mAlbumHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchView.setVisibility(View.VISIBLE);
                mChooseFolder.setVisibility(View.GONE);
                mQueryType = QUERY_TYPE_ALBUM;
                mQueryTypeText.setText(R.string.local_query_album);
                clearList();
                updateTabs();
                doQuery(mCurrentQueryText, 0);
            }
        });
        mAlbumHeaderBar = findViewById(R.id.query_album_bar);

        mTrackHeader = (ImageView) findViewById(R.id.query_track);
        mTrackHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchView.setVisibility(View.VISIBLE);
                mChooseFolder.setVisibility(View.GONE);
                mQueryType = QUERY_TYPE_TRACK;
                mQueryTypeText.setText(R.string.local_query_track);
                clearList();
                updateTabs();
                doQuery(mCurrentQueryText, 0);
            }
        });
        mTrackHeaderBar = findViewById(R.id.query_track_bar);

        mFolderHeader = (ImageView) findViewById(R.id.query_folder);
        mFolderHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchView.setVisibility(View.INVISIBLE);
                mChooseFolder.setVisibility(View.VISIBLE);
                mQueryType = QUERY_TYPE_FOLDER;
                mQueryTypeText.setText(R.string.local_query_folder);
                clearList();
                updateTabs();
                doQuery(mCurrentQueryText, 0);
            }
        });
        mFolderHeaderBar = findViewById(R.id.query_folder_bar);

        updateTabs();

        mQueryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                QueryItem queryItem = mAdapter.getItem(position);
                if (queryItem.mQueryType != QUERY_TYPE_UNKNOWN) {
                    mSelectedUri = queryItem.mUri;
                    addToRecents(mSelectedUri);
                    Intent intent = new Intent();
                    intent.putExtra(AlarmConstants.DATA_ALARM_EXTRA_URI, mSelectedUri);
                    intent.putExtra(AlarmConstants.DATA_ALARM_EXTRA_NAME, queryItem.mName);
                    intent.putExtra(AlarmConstants.DATA_ALARM_EXTRA_TYPE, queryItem.mQueryType);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }
        });

        mQueryList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                QueryItem queryItem = mAdapter.getItem(position);
                // TODO export as spotify open URL
                return true;
            }
        });

        cacheAlarmTones();
        cacheRingtones();

        View queryArea = findViewById(R.id.query_area);
        queryArea.setVisibility(View.VISIBLE);

        checkStoragePermissions();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeAlarmTestDialog();
        closeFolderPicker();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayAdapter<QueryItem> createListAdapter() {
        return new ArrayAdapter<QueryItem>(this,
                R.layout.browse_item, mQueryResultList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View item = null;
                if (convertView == null) {
                    final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    item = inflater.inflate(R.layout.browse_item, null);
                } else {
                    item = convertView;
                }
                TextView title = (TextView) item.findViewById(R.id.item_text);
                TextView subTitle = (TextView) item.findViewById(R.id.item_subtext);
                ImageView icon = (ImageView) item.findViewById(R.id.item_icon);
                ImageView playIcon = (ImageView) item.findViewById(R.id.item_play);

                final QueryItem queryItem = mQueryResultList.get(position);
                title.setText(queryItem.mName);
                if (!TextUtils.isEmpty(queryItem.mSubText)) {
                    subTitle.setVisibility(View.VISIBLE);
                    subTitle.setText(queryItem.mSubText);
                } else {
                    subTitle.setVisibility(View.GONE);
                }
                int iconId = queryItem.mIconId;
                icon.setImageResource(iconId);

                playIcon.setVisibility(queryItem.mQueryType != QUERY_TYPE_UNKNOWN ? View.VISIBLE : View.GONE);
                playIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        playUri(queryItem.mUri, queryItem.mName, queryItem.mIconId);
                    }
                });
                return item;
            }
        };
    }

    private void clearList() {
        mQueryResultList.clear();
        mAdapter.notifyDataSetChanged();
        stopProgress();
    }

    private void startProgress() {
        mProgress.setVisibility(View.VISIBLE);
    }

    private void stopProgress() {
        mProgress.setVisibility(View.GONE);
    }

    private static Uri getUriFromCursor(Cursor cursor) {
        return ContentUris.withAppendedId(Uri.parse(cursor.getString(RingtoneManager.URI_COLUMN_INDEX)), cursor
                .getLong(RingtoneManager.ID_COLUMN_INDEX));
    }

    private void searchAlarms(String query, final int startIndex) {
        startProgress();
        mQueryResultList.addAll(mAlarms);
        mAdapter.notifyDataSetChanged();
        mQueryList.setSelection(0);
        stopProgress();
    }

    private void searchRingtones(String query, final int startIndex) {
        startProgress();
        mQueryResultList.addAll(mRingtones);
        mAdapter.notifyDataSetChanged();
        mQueryList.setSelection(0);
        stopProgress();
    }

    private void searchAlbums(final String query, final int startIndex) {
        if (!mHasStoragePerms) {
            return;
        }
        startProgress();
        final List<QueryItem> queryResultList = new ArrayList<QueryItem>();
        final int startQueryType = mQueryType;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                String[] projection = {
                        MediaStore.Audio.Albums._ID,
                        MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Albums.ARTIST
                };

                String selection = null;
                if (!TextUtils.isEmpty(query)) {
                    selection = MediaStore.Audio.Albums.ALBUM + " LIKE '%" + query + "%' ";
                }
                Cursor c = getContentResolver().query(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        null,
                        MediaStore.Audio.Albums.ALBUM);

                while (c.moveToNext()) {
                    Uri album = Uri.withAppendedPath(
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            String.valueOf(c.getLong(0)));
                    String title = c.getString(1);
                    String artist = c.getString(2);
                    if (!TextUtils.isEmpty(title)) {
                        QueryItem item = new QueryItem();
                        item.mName = title;
                        item.mUri = album.toString();
                        item.mSubText = artist;
                        item.mQueryType = QUERY_TYPE_ALBUM;
                        item.mIconId = R.drawable.ic_album;
                        queryResultList.add(item);
                    }
                }
                c.close();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (mQueryType == startQueryType) {
                    mQueryResultList.clear();
                    mQueryResultList.addAll(queryResultList);
                    mAdapter.notifyDataSetChanged();
                    mQueryList.setSelection(0);
                }
                stopProgress();
            }
        }.execute();
    }

    private void searchArtists(final String query, final int startIndex) {
        if (!mHasStoragePerms) {
            return;
        }
        startProgress();
        final List<QueryItem> queryResultList = new ArrayList<QueryItem>();
        final int startQueryType = mQueryType;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                String[] projection = {
                        MediaStore.Audio.Artists._ID,
                        MediaStore.Audio.Artists.ARTIST
                };
                String selection = null;
                if (!TextUtils.isEmpty(query)) {
                    selection = MediaStore.Audio.Artists.ARTIST + " LIKE '%" + query + "%' ";
                }
                Cursor c = getContentResolver().query(
                        MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        null,
                        MediaStore.Audio.Artists.ARTIST);

                while (c.moveToNext()) {
                    Uri artist = Uri.withAppendedPath(
                            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                            String.valueOf(c.getLong(0)));
                    String artistName = c.getString(1);
                    if (!TextUtils.isEmpty(artistName) &&
                            !artistName.equals(MediaStore.UNKNOWN_STRING)) {
                        QueryItem item = new QueryItem();
                        item.mName = artistName;
                        item.mUri = artist.toString();
                        item.mQueryType = QUERY_TYPE_ARTIST;
                        item.mIconId = R.drawable.ic_artist;
                        queryResultList.add(item);
                    }
                }
                c.close();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (mQueryType == startQueryType) {
                    mQueryResultList.clear();
                    mQueryResultList.addAll(queryResultList);
                    mAdapter.notifyDataSetChanged();
                    mQueryList.setSelection(0);
                }
                stopProgress();
            }
        }.execute();
    }

    private void searchTracks(final String query, final int startIndex) {
        if (!mHasStoragePerms) {
            return;
        }
        startProgress();
        final List<QueryItem> queryResultList = new ArrayList<QueryItem>();
        final int startQueryType = mQueryType;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

                String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM
                };

                if (!TextUtils.isEmpty(query)) {
                    selection = selection + " AND " + MediaStore.Audio.Media.TITLE + " LIKE '%" + query + "%' ";
                }
                Cursor c = getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        null,
                        MediaStore.Audio.Media.TITLE);

                while (c.moveToNext()) {
                    Uri track = Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(c.getLong(0)));
                    String trackTitle = c.getString(1);
                    String artistName = c.getString(2);
                    String albumName = c.getString(3);

                    if (!TextUtils.isEmpty(trackTitle)) {
                        QueryItem item = new QueryItem();
                        item.mName = trackTitle;
                        if (!artistName.equals(MediaStore.UNKNOWN_STRING)) {
                            item.mSubText = artistName + " - " + albumName;
                        } else {
                            item.mSubText = albumName;
                        }
                        item.mUri = track.toString();
                        item.mQueryType = QUERY_TYPE_TRACK;
                        item.mIconId = R.drawable.ic_track;
                        queryResultList.add(item);
                    }
                }
                c.close();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (mQueryType == startQueryType) {
                    mQueryResultList.clear();
                    mQueryResultList.addAll(queryResultList);
                    mAdapter.notifyDataSetChanged();
                    mQueryList.setSelection(0);
                }
                stopProgress();
            }
        }.execute();
    }

    private boolean resolveAlbum(String album, QueryItem item) {
        if (!mHasStoragePerms) {
            return false;
        }
        String albumId = Uri.parse(album).getLastPathSegment();
        String selection = MediaStore.Audio.Media.ALBUM_ID + " = " + Integer.valueOf(albumId).intValue();

        String[] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST
        };

        Cursor c = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        try {
            while (c.moveToNext()) {
                String title = c.getString(1);
                String artist = c.getString(2);
                item.mName = title;
                item.mSubText = artist;
                item.mQueryType = QUERY_TYPE_ALBUM;
                item.mIconId = R.drawable.ic_album;
                return true;
            }
        } finally {
            c.close();
        }
        return false;
    }

    private void resolveRecents() {
        startProgress();
        final int startQueryType = mQueryType;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                List<QueryItem> safeIter = new ArrayList<QueryItem>();
                safeIter.addAll(mQueryResultListCopy);

                for (QueryItem item : safeIter) {
                    if (!resolveUri(item.mUri, item)) {
                        mQueryResultListCopy.remove(item);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (mQueryType == startQueryType) {
                    mQueryResultList.clear();
                    mQueryResultList.addAll(mQueryResultListCopy);
                    saveRecents(mQueryResultList);
                    mAdapter.notifyDataSetChanged();
                }
                stopProgress();
            }
        }.execute();
    }

    private boolean resolveArtist(String artist, QueryItem item) {
        if (!mHasStoragePerms) {
            return false;
        }
        String artistId = Uri.parse(artist).getLastPathSegment();
        String selection = MediaStore.Audio.Media.ARTIST_ID + " = " + Integer.valueOf(artistId).intValue();

        String[] projection = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST
        };

        Cursor c = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        try {
            while (c.moveToNext()) {
                String artistName = c.getString(1);
                item.mName = artistName;
                item.mQueryType = QUERY_TYPE_ARTIST;
                item.mIconId = R.drawable.ic_artist;
                return true;
            }
        } finally {
            c.close();
        }
        return false;
    }

    private boolean resolveTrack(String track, QueryItem item) {
        if (!mHasStoragePerms) {
            return false;
        }
        String trackId = Uri.parse(track).getLastPathSegment();
        String selection = MediaStore.Audio.Media._ID + " = " + Integer.valueOf(trackId).intValue();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM
        };

        Cursor c = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        try {
            while (c.moveToNext()) {
                String title = c.getString(1);
                String artistName = c.getString(2);
                String albumName = c.getString(3);

                item.mName = title;
                item.mSubText = artistName + " - " + albumName;
                item.mQueryType = QUERY_TYPE_TRACK;
                item.mIconId = R.drawable.ic_track;
                return true;
            }
        } finally {
            c.close();
        }
        return false;
    }

    private boolean resolveFolder(String folder, QueryItem item) {
        String path = Uri.parse(folder).getPath();
        File f = new File(path);
        if (f.isDirectory() && f.exists()) {
            item.mName = Uri.parse(folder).getLastPathSegment();
            item.mQueryType = QUERY_TYPE_FOLDER;
            item.mIconId = R.drawable.ic_folder;
            return true;
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mCurrentQueryText = newText;
        clearList();
        doQuery(newText, 0);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private void doQuery(String query, int startIndex) {
        switch (mQueryType) {
            case QUERY_TYPE_ALARM:
                clearList();
                searchAlarms(query, startIndex);
                break;
            case QUERY_TYPE_RINGTONE:
                clearList();
                searchRingtones(query, startIndex);
                break;
            case QUERY_TYPE_ALBUM:
                clearList();
                searchAlbums(query, startIndex);
                break;
            case QUERY_TYPE_ARTIST:
                clearList();
                searchArtists(query, startIndex);
                break;
            case QUERY_TYPE_TRACK:
                clearList();
                searchTracks(query, startIndex);
                break;
            case QUERY_TYPE_FOLDER:
                clearList();
                break;
            case QUERY_TYPE_RECENT:
                clearList();
                loadRecents();
                break;
        }
    }

    private void resolveUri(final String uri, View view) {
        final TextView title = (TextView) view.findViewById(R.id.item_text);
        final TextView subTitle = (TextView) view.findViewById(R.id.item_subtext);
        final ImageView icon = (ImageView) view.findViewById(R.id.item_icon);
        final ImageView playIcon = (ImageView) view.findViewById(R.id.item_play);

        boolean unknownTone = true;
        int iconId = -1;
        String alarmTitle = isAlarm(uri);
        if (alarmTitle != null) {
            iconId = R.drawable.ic_alarm;
            icon.setImageResource(iconId);
            title.setText(alarmTitle);
            subTitle.setVisibility(View.GONE);
            playIcon.setVisibility(View.VISIBLE);
            unknownTone = false;
        }
        if (unknownTone) {
            String ringtoneTitle = isRingtone(uri);
            if (ringtoneTitle != null) {
                iconId = R.drawable.ic_bell;
                icon.setImageResource(iconId);
                title.setText(ringtoneTitle);
                subTitle.setVisibility(View.GONE);
                playIcon.setVisibility(View.VISIBLE);
                unknownTone = false;
            }
        }
        if (unknownTone && Utils.isLocalAlbumUri(uri)) {
            QueryItem queryItem = new QueryItem();
            queryItem.mQueryType = QUERY_TYPE_ALBUM;
            queryItem.mUri = uri;
            if (resolveAlbum(uri, queryItem)) {
                iconId = queryItem.mIconId;
                icon.setImageResource(iconId);
                title.setText(queryItem.mName);
                subTitle.setText(queryItem.mSubText);
                playIcon.setVisibility(View.VISIBLE);
                unknownTone = false;
            }
        }
        if (unknownTone && Utils.isLocalArtistUri(uri)) {
            QueryItem queryItem = new QueryItem();
            queryItem.mQueryType = QUERY_TYPE_ARTIST;
            queryItem.mUri = uri;
            if (resolveArtist(uri, queryItem)) {
                iconId = queryItem.mIconId;
                icon.setImageResource(iconId);
                title.setText(queryItem.mName);
                subTitle.setVisibility(View.GONE);
                playIcon.setVisibility(View.VISIBLE);
                unknownTone = false;
            }
        }
        if (unknownTone && Utils.isLocalTrackUri(uri)) {
            QueryItem queryItem = new QueryItem();
            queryItem.mQueryType = QUERY_TYPE_TRACK;
            queryItem.mUri = uri;
            if (resolveTrack(uri, queryItem)) {
                iconId = queryItem.mIconId;
                icon.setImageResource(iconId);
                title.setText(queryItem.mName);
                subTitle.setText(queryItem.mSubText);
                playIcon.setVisibility(View.VISIBLE);
                unknownTone = false;
            }
        }
        if (unknownTone && Utils.isFolderUri(uri)) {
            QueryItem queryItem = new QueryItem();
            queryItem.mQueryType = QUERY_TYPE_FOLDER;
            queryItem.mUri = uri;
            if (resolveFolder(uri, queryItem)) {
                iconId = queryItem.mIconId;
                icon.setImageResource(iconId);
                title.setText(queryItem.mName);
                subTitle.setVisibility(View.GONE);
                playIcon.setVisibility(View.VISIBLE);
                unknownTone = false;
            }
        }

        if (unknownTone || TextUtils.isEmpty(title.getText())) {
            mCurrentUri.setVisibility(View.INVISIBLE);
        }
        final int iconIdFinal = iconId;
        playIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playUri(uri, title.getText().toString(), iconIdFinal);
            }
        });
    }

    private boolean resolveUri(final String uri, final QueryItem queryItem) {
        queryItem.mUri = uri;

        boolean unknownTone = true;
        String alarmTitle = isAlarm(uri);
        if (alarmTitle != null) {
            queryItem.mName = alarmTitle;
            queryItem.mIconId = R.drawable.ic_alarm;
            queryItem.mQueryType = QUERY_TYPE_ALARM;
            unknownTone = false;
        }
        if (unknownTone) {
            String ringtoneTitle = isRingtone(uri);
            if (ringtoneTitle != null) {
                queryItem.mName = ringtoneTitle;
                queryItem.mIconId = R.drawable.ic_bell;
                queryItem.mQueryType = QUERY_TYPE_RINGTONE;
                unknownTone = false;
            }
        }
        if (unknownTone && Utils.isLocalAlbumUri(uri)) {
            if (resolveAlbum(uri, queryItem)) {
                unknownTone = false;
            }
        }
        if (unknownTone && Utils.isLocalArtistUri(uri)) {
            if (resolveArtist(uri, queryItem)) {
                unknownTone = false;
            }
        }
        if (unknownTone && Utils.isLocalTrackUri(uri)) {
            if (resolveTrack(uri, queryItem)) {
                unknownTone = false;
            }
        }
        if (unknownTone && Utils.isFolderUri(uri)) {
            if (resolveFolder(uri, queryItem)) {
                unknownTone = false;
            }
        }
        if (unknownTone || TextUtils.isEmpty(queryItem.mName)) {
            queryItem.mName = getResources().getString(R.string.alarm_uri_unkown);
            queryItem.mQueryType = QUERY_TYPE_UNKNOWN;
            return false;
        }
        return true;
    }

    private void playUri(String uri, String name, int iconId) {
        if (mAlarm != null) {
            closeAlarmTestDialog();
            mAlarm.alert = Uri.parse(uri);
            final AlarmTestDialog fragment = AlarmTestDialog.newInstance(mAlarm, mPreAlarm,
                    name, iconId);
            fragment.show(getFragmentManager(), "alarm_test");
        }
    }


    private void closeAlarmTestDialog() {
        final Fragment prev = getFragmentManager().findFragmentByTag("alarm_test");
        if (prev != null) {
            ((DialogFragment) prev).dismiss();
        }
    }

    private void addToRecents(String uri) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String saved = sharedPref.getString(PREF_RECENT_URI, null);
        List<String> recentList = new ArrayList<String>();

        if (saved != null) {
            String[] savedParts = saved.split("\\|\\|");
            recentList.addAll(Arrays.asList(savedParts));
            int idx = recentList.indexOf(uri);
            if (idx != -1) {
                // always order in front if selected again
                recentList.remove(idx);
            }
        }
        recentList.add(0, uri);
        if (recentList.size() > RECENT_SIZE) {
            recentList = recentList.subList(0, RECENT_SIZE);
        }
        String recentListString = TextUtils.join("||", recentList);
        sharedPref.edit().putString(PREF_RECENT_URI, recentListString).commit();
    }

    private void saveRecents(List<QueryItem> recentList) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String saved = sharedPref.getString(PREF_RECENT_URI, null);

        List<String> recentStringList = new ArrayList<String>();
        for (QueryItem item : recentList) {
            recentStringList.add(item.mUri);
        }
        String recentListString = TextUtils.join("||", recentStringList);
        if (!saved.equals(recentListString)) {
            sharedPref.edit().putString(PREF_RECENT_URI, recentListString).commit();
        }
    }

    private void loadRecents() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String saved = sharedPref.getString(PREF_RECENT_URI, null);
        if (saved != null) {
            startProgress();
            String[] savedParts = saved.split("\\|\\|");
            mQueryResultListCopy.clear();
            for (String uri : savedParts) {
                QueryItem queryItem = new QueryItem();
                queryItem.mQueryType = QUERY_TYPE_UNKNOWN;
                queryItem.mUri = uri;
                mQueryResultListCopy.add(queryItem);
            }
            resolveRecents();
        }
    }

    private void updateTabs() {
        switch (mQueryType) {
            case QUERY_TYPE_ALARM:
                mRecentHeader.setImageResource(R.drawable.ic_star_gray);
                mAlarmHeader.setImageResource(R.drawable.ic_alarm_white);
                mRingtoneHeader.setImageResource(R.drawable.ic_bell_gray);
                mArtistHeader.setImageResource(R.drawable.ic_artist_gray);
                mAlbumHeader.setImageResource(R.drawable.ic_album_gray);
                mTrackHeader.setImageResource(R.drawable.ic_track_gray);
                mFolderHeader.setImageResource(R.drawable.ic_folder_gray);
                mRecentHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlarmHeaderBar.setBackgroundColor(getResources().getColor(R.color.white));
                mRingtoneHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mArtistHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlbumHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mTrackHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mFolderHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                break;
            case QUERY_TYPE_RINGTONE:
                mRecentHeader.setImageResource(R.drawable.ic_star_gray);
                mAlarmHeader.setImageResource(R.drawable.ic_alarm_gray);
                mRingtoneHeader.setImageResource(R.drawable.ic_bell_white);
                mArtistHeader.setImageResource(R.drawable.ic_artist_gray);
                mAlbumHeader.setImageResource(R.drawable.ic_album_gray);
                mTrackHeader.setImageResource(R.drawable.ic_track_gray);
                mFolderHeader.setImageResource(R.drawable.ic_folder_gray);
                mRecentHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlarmHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRingtoneHeaderBar.setBackgroundColor(getResources().getColor(R.color.white));
                mArtistHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlbumHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mTrackHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mFolderHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                break;
            case QUERY_TYPE_RECENT:
                mRecentHeader.setImageResource(R.drawable.ic_star_white);
                mAlarmHeader.setImageResource(R.drawable.ic_alarm_gray);
                mRingtoneHeader.setImageResource(R.drawable.ic_bell_gray);
                mArtistHeader.setImageResource(R.drawable.ic_artist_gray);
                mAlbumHeader.setImageResource(R.drawable.ic_album_gray);
                mTrackHeader.setImageResource(R.drawable.ic_track_gray);
                mFolderHeader.setImageResource(R.drawable.ic_folder_gray);
                mRecentHeaderBar.setBackgroundColor(getResources().getColor(R.color.white));
                mAlarmHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRingtoneHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mArtistHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlbumHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mTrackHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mFolderHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                break;
            case QUERY_TYPE_ARTIST:
                mRecentHeader.setImageResource(R.drawable.ic_star_gray);
                mAlarmHeader.setImageResource(R.drawable.ic_alarm_gray);
                mRingtoneHeader.setImageResource(R.drawable.ic_bell_gray);
                mArtistHeader.setImageResource(R.drawable.ic_artist_white);
                mAlbumHeader.setImageResource(R.drawable.ic_album_gray);
                mTrackHeader.setImageResource(R.drawable.ic_track_gray);
                mFolderHeader.setImageResource(R.drawable.ic_folder_gray);
                mRecentHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlarmHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRingtoneHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mArtistHeaderBar.setBackgroundColor(getResources().getColor(R.color.white));
                mAlbumHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mTrackHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mFolderHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                break;
            case QUERY_TYPE_ALBUM:
                mRecentHeader.setImageResource(R.drawable.ic_star_gray);
                mAlarmHeader.setImageResource(R.drawable.ic_alarm_gray);
                mRingtoneHeader.setImageResource(R.drawable.ic_bell_gray);
                mArtistHeader.setImageResource(R.drawable.ic_artist_gray);
                mAlbumHeader.setImageResource(R.drawable.ic_album_white);
                mTrackHeader.setImageResource(R.drawable.ic_track_gray);
                mFolderHeader.setImageResource(R.drawable.ic_folder_gray);
                mRecentHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlarmHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRingtoneHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mArtistHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlbumHeaderBar.setBackgroundColor(getResources().getColor(R.color.white));
                mTrackHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mFolderHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                break;
            case QUERY_TYPE_TRACK:
                mRecentHeader.setImageResource(R.drawable.ic_star_gray);
                mAlarmHeader.setImageResource(R.drawable.ic_alarm_gray);
                mRingtoneHeader.setImageResource(R.drawable.ic_bell_gray);
                mArtistHeader.setImageResource(R.drawable.ic_artist_gray);
                mAlbumHeader.setImageResource(R.drawable.ic_album_gray);
                mTrackHeader.setImageResource(R.drawable.ic_track_white);
                mFolderHeader.setImageResource(R.drawable.ic_folder_gray);
                mRecentHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlarmHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRingtoneHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mArtistHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlbumHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mTrackHeaderBar.setBackgroundColor(getResources().getColor(R.color.white));
                mFolderHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                break;
            case QUERY_TYPE_FOLDER:
                mRecentHeader.setImageResource(R.drawable.ic_star_gray);
                mAlarmHeader.setImageResource(R.drawable.ic_alarm_gray);
                mRingtoneHeader.setImageResource(R.drawable.ic_bell_gray);
                mArtistHeader.setImageResource(R.drawable.ic_artist_gray);
                mAlbumHeader.setImageResource(R.drawable.ic_album_gray);
                mTrackHeader.setImageResource(R.drawable.ic_track_gray);
                mFolderHeader.setImageResource(R.drawable.ic_folder_white);
                mRecentHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlarmHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRingtoneHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mArtistHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mAlbumHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mTrackHeaderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mFolderHeaderBar.setBackgroundColor(getResources().getColor(R.color.white));
                break;
        }
    }

    private void cacheAlarmTones() {
        mAlarms = new ArrayList<QueryItem>();

        Cursor alarmsCursor = null;
        try {
            RingtoneManager ringtoneMgr = new RingtoneManager(this
                    .getApplicationContext());
            ringtoneMgr.setType(RingtoneManager.TYPE_ALARM);

            alarmsCursor = ringtoneMgr.getCursor();
            int alarmsCount = alarmsCursor.getCount();
            if (alarmsCount == 0 && !alarmsCursor.moveToFirst()) {
                return;
            }

            while (!alarmsCursor.isAfterLast() && alarmsCursor.moveToNext()) {
                QueryItem queryItem = new QueryItem();
                queryItem.mName = alarmsCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                queryItem.mUri = getUriFromCursor(alarmsCursor).toString();
                queryItem.mIconId = R.drawable.ic_alarm;
                mAlarms.add(queryItem);
            }
        } finally {
            if (alarmsCursor != null) {
                alarmsCursor.close();
            }
        }
    }

    private void cacheRingtones() {
        mRingtones = new ArrayList<QueryItem>();

        Cursor alarmsCursor = null;
        try {
            RingtoneManager ringtoneMgr = new RingtoneManager(this
                    .getApplicationContext());
            ringtoneMgr.setType(RingtoneManager.TYPE_RINGTONE);

            alarmsCursor = ringtoneMgr.getCursor();
            int alarmsCount = alarmsCursor.getCount();
            if (alarmsCount == 0 && !alarmsCursor.moveToFirst()) {
                return;
            }

            while (!alarmsCursor.isAfterLast() && alarmsCursor.moveToNext()) {
                QueryItem queryItem = new QueryItem();
                queryItem.mName = alarmsCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                queryItem.mUri = getUriFromCursor(alarmsCursor).toString();
                queryItem.mIconId = R.drawable.ic_bell;
                mRingtones.add(queryItem);
            }
        } finally {
            if (alarmsCursor != null) {
                alarmsCursor.close();
            }
        }
    }

    private String isAlarm(String uri) {
        for (QueryItem i : mAlarms) {
            if (i.mUri.equals(uri)) {
                return i.mName;
            }
        }
        return null;
    }

    private String isRingtone(String uri) {
        for (QueryItem i : mRingtones) {
            if (i.mUri.equals(uri)) {
                return i.mName;
            }
        }
        return null;
    }

    private void launchFolderPicker() {
        closeFolderPicker();

        final DirectoryChooserDialog fragment = DirectoryChooserDialog.newInstance(this);
        fragment.show(getFragmentManager(), "choose_folder");
    }

    private void closeFolderPicker() {
        final Fragment prev = getFragmentManager().findFragmentByTag("choose_folder");
        if (prev != null) {
            ((DialogFragment) prev).dismiss();
        }
    }

    @Override
    public void onChooseDirOk(Uri chosenDir) {
        mQueryResultList.clear();
        QueryItem item = new QueryItem();
        item.mUri = chosenDir.toString();
        resolveFolder(chosenDir.toString(), item);
        mQueryResultList.add(item);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onChooseDirCancel() {
    }

    private void checkStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            mHasStoragePerms = true;
            doInit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mHasStoragePerms = true;
                }
                doInit();
            }
            return;
        }
    }

    private void doInit() {
        doQuery(mCurrentQueryText, 0);
        if (mSelectedUri != null) {
            resolveUri(mSelectedUri, mCurrentUri);
        }
    }
}

