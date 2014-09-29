package codingpark.net.cheesecloud.view;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import codingpark.net.cheesecloud.Configs;
import codingpark.net.cheesecloud.DevicePath;
import codingpark.net.cheesecloud.R;
import codingpark.net.cheesecloud.handle.UploadHandler;
import codingpark.net.cheesecloud.handle.FileManager;
import codingpark.net.cheesecloud.model.CatalogList;
import codingpark.net.cheesecloud.utils.TypeFilter;

/**
 *
 */
public final class UploadActivity extends ListActivity {

    private FileManager mFileMag                        = null;
    private UploadHandler mHandler                       = null;
    private UploadHandler.TableRow mTable                = null;
    private CatalogList mCataList                       = null;
    private DevicePath mDevicePath                      = null;

    private SharedPreferences mSettings                 = null;
    // UI element to display current full path
    private TextView  mPathLabel                        = null;
    // UI element to display current selected file name
    private TextView  mDetailLabel                      = null;

    private String TAG                      = "UploadActivity";
    private String openType                 = null;
    private File openFile                   = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        Don't hide actionbar, need it to display menu
        if(android.os.Build.VERSION.SDK_INT < 11) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            ActionBar actionBar = getActionBar();
            actionBar.hide();
        }
        */

        setContentView(R.layout.activity_upload);

        /* Get system preferences: hide/thumb/color/sort */
        mSettings           = getSharedPreferences(Configs.PREFS_NAME, 0);
        boolean hide        = mSettings.getBoolean(Configs.PREFS_HIDDEN, false);
        boolean thumb       = mSettings.getBoolean(Configs.PREFS_THUMBNAIL, true);
        int color           = mSettings.getInt(Configs.PREFS_COLOR, -1);
        int sort            = mSettings.getInt(Configs.PREFS_SORT, 1);

        // 1. Initial FileManager utility
        // 2. Set FileManager utility work parameter
        mFileMag = new FileManager(this);
        mFileMag.setShowHiddenFiles(hide);
        mFileMag.setSortType(sort);

        // Initial CatalogList
        mCataList = new CatalogList(this);
        // Intial DevicePath
        mDevicePath = new DevicePath(this);

        // 1. Initial EventHandler
        // 2. Set EventHandler work parameter(text color/show thumbnail)
        // 3. Create ListAdapter
        mHandler = new UploadHandler(UploadActivity.this, mFileMag, mCataList);
        mHandler.setTextColor(color);
        mHandler.setShowThumbnails(thumb);
        mTable = mHandler.new TableRow();

        /**
         * sets the ListAdapter for our ListActivity and
         * gives our EventHandler class the same adapter
         */
        mHandler.setListAdapter(mTable);
        setListAdapter(mTable);
        getListView().setOnItemLongClickListener(mHandler);
        
        mDetailLabel = (TextView)findViewById(R.id.detail_label);
        mPathLabel = (TextView)findViewById(R.id.path_label);
        mHandler.setUpdateLabels(mPathLabel, mDetailLabel);
		
        /*
         * Start refresh list
         *      then: list storage list
         */
        mPathLabel.setText(mFileMag.getCurrentDir());
        mHandler.updateDirectory(mFileMag.getHomeDir(FileManager.ROOT_FLASH));
        getFocusForButton(R.id.home_flash_button);


        /*
        int[] img_button_id = {
                R.id.home_flash_button,
                R.id.back_button,
                R.id.image_button,
                R.id.movie_button};


        ImageButton[] bimg = new ImageButton[img_button_id.length];
        */
    }

    private void getFocusForButton(int id)
    {
        View v = findViewById(id);
        mHandler.getInitView(v);
        v.setSelected(true);
        mHandler.UpdateButtons(UploadHandler.DISABLE_TOOLBTN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String getCurrentFileName(int position){
        return mHandler.getCurrentFilePath(position);
    }
    /**
     *  To add more functionality and let the user interact with more
     *  file types, this is the function to add the ability.
     */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        final String item = getCurrentFileName(position);
        File file = new File(item);
        boolean multiSelect = mHandler.isMultiSelected();

        String item_ext = null;

        try {
            item_ext = item.substring(item.lastIndexOf(".") + 1, item.length());

        } catch(IndexOutOfBoundsException e) {
            item_ext = "";
        }
    	
    	/*
    	 * If the user has multi-select on, we just need to record the file
    	 * not make an intent for it.
    	 */
        if(multiSelect) {
            mTable.addMultiPosition(position, file.getPath());

        } else {
            if (file.isDirectory()) {
                if(file.canRead()) {
                    mHandler.updateDirectory(mFileMag.getNextDir(item));
                    mPathLabel.setText(mFileMag.getCurrentDir());
		    		
                } else {
                    Toast.makeText(this, "Can't read folder due to permissions",
                            Toast.LENGTH_SHORT).show();
                }
                if(mFileMag.isRoot()){
                    mHandler.UpdateButtons(UploadHandler.DISABLE_TOOLBTN);
                }else{
                    mHandler.UpdateButtons(UploadHandler.ENABLE_TOOLBTN);
                }
            }
	    	
	    	/*music file selected--add more audio formats*/
            else if (TypeFilter.getInstance().isMusicFile(item_ext)) {
                Intent picIntent = new Intent();
                picIntent.setAction(android.content.Intent.ACTION_VIEW);
                picIntent.setDataAndType(Uri.fromFile(file), "audio/*");
                startActivity(picIntent);
            }
	    	
	    	/*photo file selected*/
            else if(TypeFilter.getInstance().isPictureFile(item_ext)) {
                if (file.exists()) {
                        Intent picIntent = new Intent();
                        picIntent.setAction(android.content.Intent.ACTION_VIEW);
                        picIntent.setDataAndType(Uri.fromFile(file), "image/*");
                        startActivity(picIntent);
                }
            }
	    	
	    	/*video file selected--add more video formats*/
            else if(TypeFilter.getInstance().isMovieFile(item_ext)) {

                if (file.exists()) {
                        Intent movieIntent = new Intent();

                    // for VideoPlayer to create playlist
                    // movieIntent.putExtra(MediaStore.PLAYLIST_TYPE, MediaStore.PLAYLIST_TYPE_CUR_FOLDER);

                    movieIntent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
                    movieIntent.setAction(android.content.Intent.ACTION_VIEW);
                    movieIntent.setDataAndType(Uri.fromFile(file), "video/*");
                    startActivity(movieIntent);
                }
            }

	    	/*pdf file selected*/
            else if(TypeFilter.getInstance().isPdfFile(item_ext)) {

                if(file.exists()) {
                    Intent pdfIntent = new Intent();
                    pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
                    pdfIntent.setDataAndType(Uri.fromFile(file),
                            "application/pdf");

                    try {
                        startActivity(pdfIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, "Sorry, couldn't find a pdf viewer",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
	    	
	    	/*Android application file*/
            else if(TypeFilter.getInstance().isApkFile(item_ext)){

                if(file.exists()) {
                    Intent apkIntent = new Intent();
                    apkIntent.setAction(android.content.Intent.ACTION_VIEW);
                    apkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    startActivity(apkIntent);
                }
            }
	    	
	    	/* HTML file */
            else if(TypeFilter.getInstance().isHtml32File(item_ext)) {

                if(file.exists()) {
                    Intent htmlIntent = new Intent();
                    htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
                    htmlIntent.setDataAndType(Uri.fromFile(file), "text/html");

                    try {
                        startActivity(htmlIntent);
                    } catch(ActivityNotFoundException e) {
                        Toast.makeText(this, "Sorry, couldn't find a HTML viewer",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
	    	
	    	/* text file*/
            else if(TypeFilter.getInstance().isTxtFile(item_ext)) {

                if(file.exists()) {
                    Intent txtIntent = new Intent();
                    txtIntent.setAction(android.content.Intent.ACTION_VIEW);
                    txtIntent.setDataAndType(Uri.fromFile(file), "text/plain");

                    try {
                        startActivity(txtIntent);
                    } catch(ActivityNotFoundException e) {
                        txtIntent.setType("text/*");
                        startActivity(txtIntent);
                    }
                }
            }
	    	
	    	/* generic intent */
            else {
                if(file.exists()) {
                    openFile = file;
                    selectFileType_dialog();
                }
            }
        }
    }

    /**
     * Show dialog to select which type the selected file is
     */
    private void selectFileType_dialog() {
        String mFile = UploadActivity.this.getResources().getString(R.string.open_file);
        String mText = UploadActivity.this.getResources().getString(R.string.text);
        String mAudio = UploadActivity.this.getResources().getString(R.string.audio);
        String mVideo = UploadActivity.this.getResources().getString(R.string.video);
        String mImage = UploadActivity.this.getResources().getString(R.string.image);
        CharSequence[] FileType = {mText,mAudio,mVideo,mImage};
        AlertDialog.Builder builder;
        AlertDialog dialog;
        builder = new AlertDialog.Builder(UploadActivity.this);
        builder.setTitle(mFile);
        builder.setIcon(R.drawable.help);
        builder.setItems(FileType, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent mIntent = new Intent();
                switch(which) {
                    case 0:
                        openType = "text/*";
                        break;
                    case 1:
                        openType = "audio/*";
                        break;
                    case 2:
                        openType = "video/*";
                        break;
                    case 3:
                        openType = "image/*";
                        break;
                }
                mIntent.setAction(android.content.Intent.ACTION_VIEW);
                mIntent.setDataAndType(Uri.fromFile(openFile), openType);
                try {
                    startActivity(mIntent);
                } catch(ActivityNotFoundException e) {
                    Toast.makeText(UploadActivity.this, "Sorry, couldn't find anything " +
                                    "to open " + openFile.getName(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog = builder.create();
        dialog.show();
    }


    /**
     * This will check if the user is at root directory. If so, if they press back
     * again, it will close the application.
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        String current = mFileMag.getCurrentDir();

        if(keycode == KeyEvent.KEYCODE_BACK &&
                !(mFileMag.isRoot()) ) {
            if(mHandler.isMultiSelected()) {
                mTable.killMultiSelect(true);
                Toast.makeText(UploadActivity.this, getResources().getString(R.string.Multi_select_off), Toast.LENGTH_SHORT).show();
            }

            mHandler.updateDirectory(mFileMag.getPreviousDir());
            mPathLabel.setText(mFileMag.getCurrentDir());
            if(mFileMag.isRoot()){
                mHandler.UpdateButtons(UploadHandler.DISABLE_TOOLBTN);
            }else{
                mHandler.UpdateButtons(UploadHandler.ENABLE_TOOLBTN);
            }
            return true;

        } else if(keycode == KeyEvent.KEYCODE_BACK && //mUseBackKey &&
                mFileMag.isRoot() ) {
            mPathLabel.setText(mFileMag.getCurrentDir());
            finish();
            return false;

        }
        return false;
    }

}
