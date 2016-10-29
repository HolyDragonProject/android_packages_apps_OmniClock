/*
 *  Copyright (C) 2014 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.omnirom.deskclock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DirectoryChooserDialog extends DialogFragment
    implements DialogInterface.OnClickListener {

    private String mSDCardDirectory;
    private String mCurrentDir;
    private List<File> mSubDirs;
    private ArrayAdapter<File> mListAdapter;
    private ListView mListView;
    private String mTag;
    private int mTextColor;
    private int mTextColorDisabled;
    private ChosenDirectoryListener mListener;

    public interface ChosenDirectoryListener {
        public void onChooseDirOk(Uri chosenDir);

        public void onChooseDirCancel();
    }

    public static DirectoryChooserDialog newInstance(ChosenDirectoryListener listener) {
        DirectoryChooserDialog fragment = new DirectoryChooserDialog();
        fragment.setChoosenListener(listener);
        return fragment;
    }

    public DirectoryChooserDialog() {
        mSDCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        try {
            mSDCardDirectory = new File(mSDCardDirectory).getCanonicalPath();
        } catch (IOException ioe) {
        }
    }

    public void setChoosenListener(ChosenDirectoryListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.folder_dialog_title)
                .setPositiveButton("", this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(createDialogView());

        return builder.create();
    }

    private List<File> getDirectories(String dir) {
        List<File> dirs = new ArrayList<File>();

        try {
            File dirFile = new File(dir);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return dirs;
            }

            for (File file : dirFile.listFiles()) {
                if (!file.getName().startsWith(".")) {
                    dirs.add(file);
                }
            }
        } catch (Exception e) {
        }

        Collections.sort(dirs, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile()) {
                    return -1;
                }
                if (o2.isDirectory() && o1.isFile()) {
                    return 1;
                }
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        if (!dir.equals(mSDCardDirectory)) {
            dirs.add(0, new File(".."));
        }
        return dirs;
    }

    private void updateDirectory() {
        mSubDirs.clear();
        mSubDirs.addAll(getDirectories(mCurrentDir));
        mListAdapter.notifyDataSetChanged();
        boolean enableOk = !mCurrentDir.equals(mSDCardDirectory);
        ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setText(getResources().getString(android.R.string.ok));
        ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(enableOk ? View.VISIBLE : View.GONE);
    }

    private ArrayAdapter<File> createListAdapter(List<File> items) {
        return new ArrayAdapter<File>(getActivity(),
                R.layout.folder_item, R.id.folder_name, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View item = null;
                if (convertView == null){
                    final LayoutInflater inflater = (LayoutInflater) getActivity()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    item = inflater.inflate(R.layout.folder_item, null);
                } else {
                    item = convertView;
                }
                TextView tv = (TextView) item.findViewById(R.id.folder_name);
                File f = mSubDirs.get(position);
                tv.setText(f.getName());
                if (f.isFile()) {
                    tv.setTextColor(mTextColorDisabled);
                } else {
                    tv.setTextColor(mTextColor);
                }
                return item;
            }
        };
    }

    private View createDialogView() {
        final LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater
                .inflate(R.layout.folder_dialog, null);

        TypedValue outValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.textColorPrimary, outValue, true);
        mTextColor = getActivity().getResources().getColor(outValue.resourceId);
        boolean light = Utils.isLightTheme(getActivity());
        if (light) {
            mTextColorDisabled = getActivity().getResources().getColor(R.color.folder_dialog_file_color);
        } else {
            mTextColorDisabled = getActivity().getResources().getColor(R.color.folder_dialog_file_color_dark);
        }

        mCurrentDir = mSDCardDirectory;
        mSubDirs = getDirectories(mSDCardDirectory);

        mListAdapter = createListAdapter(mSubDirs);
        mListView = (ListView) view.findViewById(R.id.folders);
        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                File f = mListAdapter.getItem(position);
                if (f.isFile()) {
                    return;
                }
                if (f.getName().equals("..")) {
                    if (!mCurrentDir.equals(mSDCardDirectory)) {
                        // Navigate back to an upper directory
                        mCurrentDir = new File(mCurrentDir).getParent();
                    }
                } else {
                    // Navigate into the sub-directory
                    mCurrentDir = mListAdapter.getItem(position).getAbsolutePath();
                }
                updateDirectory();
            }
        });

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Uri uri = Uri.fromFile(new File(mCurrentDir));
            mListener.onChooseDirOk(uri);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            mListener.onChooseDirCancel();
        }
    }
}
