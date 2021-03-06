package codingpark.net.cheesecloud.view;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ListFragment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import codingpark.net.cheesecloud.R;

import codingpark.net.cheesecloud.handle.OnFragmentInteractionListener;
import codingpark.net.cheesecloud.handle.OnKeyDownListener;
import codingpark.net.cheesecloud.handle.OnSelectUploadChangedListener;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class FragmentSelectUploadImage extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnKeyDownListener {

    private OnSelectUploadChangedListener mListener;

    private Context mContext = null;

    public static final String TAG = FragmentSelectUploadImage.class.getSimpleName();
    private ContentResolver cr      = null;

    private LinearLayout mPathBar                   = null;

    private PathBarItemClickListener mPathBatItemListener       = null;

    private LinearLayout mListContainer                 = null;
    private ProgressBar mLoadingView                    = null;

    private String[] image_projection = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.MINI_THUMB_MAGIC
    };

    /**
     * List the item of the selected category in by date order
     */
    public static final int ITEM_LIST_MODE = 0;
    /**
     * List the all category by category id
     */
    public static final int CATEGORY_LIST_MODE  = 1;
    /**
     * Current list mode
     * 0: {@see ITEM_LIST_MODE}
     * 1: {@see CATEGORY_LIST_MODE}
     */
    private int mListMode                       = CATEGORY_LIST_MODE;

    /**
     * Store the {@see CategoryItem} objects that query from
     * {@link android.provider.MediaStore.Images.Media}. This
     * list will show in CATEGORY_LIST_MODE as data of ListView.
     */
    private ArrayList<ItemImage> mCategoryList       = null;
    /**
     * Store the {@see ItemImage} objects that query from
     * {@link android.provider.MediaStore.Images}, the data will filtered
     * by BUCKET_ID, this list will show in ITEM_LIST_MODE as data of ListView.
     */
    private ArrayList<ItemImage> mSubItemList = null;
    /**
     * Store the all {@see ItemImage} objects that query from
     * {@link android.provider.MediaStore.Images}.
     */
    private ArrayList<ItemImage> mAllItemList = null;
    /**
     * The {@see CATEGORY_LIST_MODE} list view adapter
     */
    private ImageCategoryAdapter mCategoryAdapter           = null;
    /**
     * The {@see IMAGE_CATEGORY_LIST_MODE} list view adapter
     */
    private ImageItemAdapter mItemAdapter                   = null;
    /**
     * The LayoutInflater object, used by ArrayAdapter to inflate view from
     * layout xml file.
     */
    private LayoutInflater mInflater                        = null;

    private ArrayList<String> mSelectedPath         = null;
    // Store user selected files index in the ListView
    private ArrayList<Integer> mSelectedPositions = null;

    private boolean isAlive                         = false;

    public static FragmentSelectUploadImage newInstance(String param1, String param2) {
        FragmentSelectUploadImage fragment = new FragmentSelectUploadImage();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FragmentSelectUploadImage() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cr = mContext.getContentResolver();
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Initial MediaStore query task
        getLoaderManager().initLoader(0, null, this);
        // Initial the two show mode data list
        mAllItemList = new ArrayList<ItemImage>();
        mCategoryList = new ArrayList<ItemImage>();
        mSubItemList = new ArrayList<ItemImage>();
        mSelectedPath = new ArrayList<String>();
        mSelectedPositions = new ArrayList<Integer>();
        // Intial the two show mode data adapter
        mCategoryAdapter = new ImageCategoryAdapter(mContext, R.layout.select_upload_image_category_mode_item_layout, mCategoryList);
        mItemAdapter = new ImageItemAdapter(mContext, R.layout.select_upload_image_item_mode_item_layout, mSubItemList);
        // Set default list adapter to CATEGORY_LIST_MODE
        mPathBatItemListener = new PathBarItemClickListener();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSelectUploadChangedListener) activity;
            mContext = activity;
            //setContentView(R.layout.select_upload_image_layout);

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_select_upload_images, null);
        mListContainer = (LinearLayout)rootView.findViewById(R.id.listcontainer);
        mLoadingView = (ProgressBar)rootView.findViewById(R.id.loading);

        if (mListMode == CATEGORY_LIST_MODE && !isAlive) {
            setLoadingViewVisible(true);
        }
        isAlive = true;
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        switch (mListMode) {
            case CATEGORY_LIST_MODE:
                setListAdapter(mCategoryAdapter);
                //mCategoryAdapter.notifyDataSetChanged();
                break;
            case ITEM_LIST_MODE:
                setListAdapter(mItemAdapter);
                //mItemAdapter.notifyDataSetChanged();
                break;
        }
        mPathBar = (LinearLayout)getView().findViewById(R.id.pathBarContainer);
        setUpdatePathBar(mPathBar);
        refreshPathBar();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isAlive = false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "onListItemClick: position=" + position);
        if (mListMode == CATEGORY_LIST_MODE) {
            setLoadingViewVisible(true);
            new LoadThumbTask(position).execute();
        } else if (mListMode == ITEM_LIST_MODE) {
            addMultiPosition(position);
        }
    }



    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String order_clause = MediaStore.Images.Media.BUCKET_ID + " ASC, "
                + MediaStore.Images.Media.DATE_TAKEN + " ASC ";
        return new CursorLoader(mContext,
                uri,
                image_projection,
                null,
                null,
                order_clause) ;
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        Log.d(TAG, "onLoadFinished: " + data.getCount());
        if (data != null)  {
            data.moveToPosition(-1);
            // Clear mAllItemList item
            mAllItemList.clear();
            // Traverse the cursor, store the data to mAllItemList
            while(data.moveToNext()) {
                /*
                Log.d(TAG, "###################################################3");
                Log.d(TAG, "ID: " + data.getInt(0));
                Log.d(TAG, "DATA: " + data.getString(1));
                Log.d(TAG, "DATE_TAKEN: " + data.getInt(2));
                Log.d(TAG, "BUCKET_DISPLAY_NAME: " + data.getString(3));
                Log.d(TAG, "BUCKET_ID: " + data.getInt(4));
                Log.d(TAG, "MINI_THUMB_MAGIC: " + data.getInt(5));
                Log.d(TAG, "###################################################3");
                */
                // Judge the image is exist? If not exists, needn't add it to mAllItemList
                if (!(new File(data.getString(1)).exists())) {
                    continue;
                }
                ItemImage item =new ItemImage();
                item.id = data.getInt(0);
                item.data = data.getString(1);
                item.date_taken = data.getInt(2);
                item.bucket_display_name = data.getString(3);
                item.bucket_id = data.getInt(4);
                item.mini_thumb_magic = data.getInt(5);
                mAllItemList.add(item);
            }
            // Traverse mAllItemList, filter by bucket id, add unique bucket to mCategoryList
            mCategoryList.clear();
            if (mAllItemList.size() > 0) {
                ItemImage r_category = null;
                try {
                    r_category = (ItemImage)mAllItemList.get(0).clone();
                    r_category.item_count = 1;
                    int r_bucket_id = mAllItemList.get(0).bucket_id;
                    mCategoryList.add(r_category);
                    for (int i = 1; i < mAllItemList.size(); i++) {
                        if (mAllItemList.get(i).bucket_id == r_bucket_id) {
                            mCategoryList.get(mCategoryList.size() - 1).item_count++;
                        }
                        else {
                            r_category = (ItemImage)mAllItemList.get(i).clone();
                            r_category.item_count = 1;
                            r_bucket_id = r_category.bucket_id;
                            mCategoryList.add(r_category);
                        }
                    }
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        for (int i = 0; i < mCategoryList.size(); i++) {
            getThumbPath(mCategoryList.get(i));
        }
        if (mListMode == CATEGORY_LIST_MODE) {
            Log.d(TAG, "Refresh list");
            Log.d(TAG, "mCategoryList: " + mCategoryList.size());
            Log.d(TAG, "mAllItemList: " + mAllItemList.size());
            mCategoryAdapter.notifyDataSetChanged();
            setLoadingViewVisible(false);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        Log.d(TAG, "setUserVisibleHint: " + isVisibleToUser);
        super.setUserVisibleHint(isVisibleToUser);
        if (mListMode == ITEM_LIST_MODE && !isVisibleToUser) {
            if ((mSelectedPositions != null) || (mSelectedPath != null)) {
                mSelectedPath.clear();
                mSelectedPositions.clear();
                mItemAdapter.notifyDataSetChanged();
            }
        }
    }

    private void addMultiPosition(int index) {
        String r_path = mSubItemList.get(index).data;
        if (mSelectedPositions.contains(index)) {
            mSelectedPositions.remove(Integer.valueOf(index));
            mSelectedPath.remove(r_path);
        } else {
            mSelectedPositions.add(index);
            mSelectedPath.add(r_path);
        }
        mItemAdapter.notifyDataSetChanged();
        if (mListener != null)
            mListener.onSelectUploadChanged(mSelectedPath);
    }

    /**
     * This will turn off multi-select and hide the multi-select buttons at the
     * bottom of the view.
     */
    public void clearMultiSelect() {

        if(mSelectedPositions != null && !mSelectedPositions.isEmpty())
            mSelectedPositions.clear();

        if(mSelectedPath != null && !mSelectedPath.isEmpty())
            mSelectedPath.clear();

        if (mListMode == ITEM_LIST_MODE) {
            mItemAdapter.notifyDataSetChanged();
            if (mListener != null)
                mListener.onSelectUploadChanged(mSelectedPath);
        }
    }

    private long lastPhotoId        = 0;
    private String getThumbPath(ItemImage item) {
        String path = "";
        Cursor cursor = null;
        try {
            cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                    cr, item.id,
                    MediaStore.Images.Thumbnails.MINI_KIND, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                String thumbPath = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Thumbnails.DATA));
                File thumb = new File(thumbPath);
                if (thumb.exists())
                    item.thumb_path = thumbPath;
                else
                    item.thumb_path = "";
                Log.d(TAG, "getThumbPath: " + item.thumb_path);
            } else {
                if (lastPhotoId == item.id) {
                    item.thumb_path = "";
                    Log.d(TAG, "getThumbPath: " + "empty");
                } else {
                    MediaStore.Images.Thumbnails.getThumbnail(cr,
                            item.id, MediaStore.Images.Thumbnails.MINI_KIND, null);
                    lastPhotoId = item.id;
                    getThumbPath(item);
                }
            }
        } finally {
            if(cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return path;
    }

    @Override
    public boolean onBackKeyDown() {
        if (mListMode == CATEGORY_LIST_MODE) {
            return false;
        } else {
            clearMultiSelect();
            mListMode = CATEGORY_LIST_MODE;
            setListAdapter(mCategoryAdapter);
            mCategoryAdapter.notifyDataSetChanged();
            refreshPathBar();
            return true;
        }
    }

    private static final class CategoryViewHolder {
        public ImageView bucketThumbView    = null;
        public TextView bucketNameView      = null;
        public TextView countView           = null;
    }

    /**
     * The {@see CATEGORY_LIST_MODE} adapter
     */
    private class ImageCategoryAdapter extends ArrayAdapter<ItemImage> {

        public ImageCategoryAdapter(Context context, int resource, List<ItemImage> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemImage item = mCategoryList.get(position);
            CategoryViewHolder holder = null;
            if (convertView == null) {
                holder = new CategoryViewHolder();
                convertView = mInflater.inflate(R.layout.select_upload_image_category_mode_item_layout, null);
                holder.bucketThumbView = (ImageView)convertView.findViewById(R.id.bucketImageView);
                holder.bucketNameView = (TextView)convertView.findViewById(R.id.bucketNameView);
                holder.countView = (TextView)convertView.findViewById(R.id.countTextView);
                convertView.setTag(holder);
            } else {
                holder = (CategoryViewHolder)convertView.getTag();
            }
            holder.bucketThumbView.setImageResource(R.drawable.ic_launcher);
            holder.bucketThumbView.setImageBitmap(MediaStore.Images.Thumbnails.getThumbnail(cr, item.id, MediaStore.Images.Thumbnails.MICRO_KIND, null));
            holder.bucketNameView.setText(item.bucket_display_name);
            holder.countView.setText(item.item_count + "");
            return convertView;
        }
    }

    private static final class ImageItemViewHolder {
        public ImageView itemThumbView      = null;
        public TextView imageNameView       = null;
        public TextView imageTakeDateView = null;
        public CheckBox imageCheckbox       = null;
    }

    /**
     * The {@SEE ITEM_LIST_MODE} adapter
     */
    private class ImageItemAdapter extends ArrayAdapter<ItemImage> {
        private CompoundButton.OnCheckedChangeListener mCheckedListener     = null;

        public ImageItemAdapter(Context context, int resource, List<ItemImage> objects) {
            super(context, resource, objects);
            mCheckedListener = new ItemCheckedListener();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemImage item = mSubItemList.get(position);
            ImageItemViewHolder holder = null;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.select_upload_image_item_mode_item_layout, null);
                holder = new ImageItemViewHolder();
                holder.itemThumbView = (ImageView)convertView.findViewById(R.id.itemImageView);
                holder.imageNameView = (TextView)convertView.findViewById(R.id.image_name_view);
                holder.imageTakeDateView = (TextView)convertView.findViewById(R.id.image_take_date_view);
                holder.imageCheckbox = (CheckBox)convertView.findViewById(R.id.image_checkbox);
                holder.imageCheckbox.setOnCheckedChangeListener(mCheckedListener);
                convertView.setTag(holder);
            } else {
                holder = (ImageItemViewHolder)convertView.getTag();
            }

            String path = item.data;
            if (path == null || path.isEmpty()) {
                holder.itemThumbView.setImageResource(R.drawable.ic_launcher);
            } else {
                holder.itemThumbView.setImageBitmap(MediaStore.Images.Thumbnails.getThumbnail(cr, item.id, MediaStore.Images.Thumbnails.MICRO_KIND, null));
            }
            path = path.substring(path.lastIndexOf("/") + 1, path.length());
            holder.imageNameView.setText(path);
            holder.imageTakeDateView.setText(item.date_taken + "");
            holder.imageCheckbox.setTag(String.valueOf(position));

            if (mSelectedPositions != null && mSelectedPositions.contains(position))
                holder.imageCheckbox.setChecked(true);
            else
                holder.imageCheckbox.setChecked(false);

            return convertView;
        }

        /**
         * This class listening ListView item's select CheckBox checked event.
         * When user checked a item, class add this item's index to {@link #mSelectedPositions},
         * and add path which the item stand for to {@link #mSelectedPath}
         */
        private class ItemCheckedListener implements CompoundButton.OnCheckedChangeListener{
            //private static final String TAG     = "ItemSelectedListener";

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Index: " + buttonView.getTag() + "\nChecked: " + isChecked);
                int r_index = Integer.valueOf(buttonView.getTag().toString());
                boolean isChanged = false;
                if (isChecked) {
                    if (!mSelectedPositions.contains(r_index)) {
                        mSelectedPositions.add(r_index);
                        mSelectedPath.add(mSubItemList.get(r_index).data);
                        isChanged = true;
                    }
                } else {
                    if (mSelectedPositions.contains(r_index)) {
                        mSelectedPositions.remove((Integer)r_index);
                        mSelectedPath.remove(mSubItemList.get(r_index).data);
                        isChanged = true;
                    }
                }
                Log.d(TAG, "Current selected items: " + mSelectedPositions.toString());
                if (isChanged && mListener != null) {
                    mListener.onSelectUploadChanged(mSelectedPath);
                }

            }
        }
    }


    /**
     * Store the image item information that query from
     * {@link android.provider.MediaStore.Images.Media}
     */
    private static class ItemImage implements Cloneable{
        /**
         * Associate with _ID(index) field of the {@link android.provider.MediaStore.Images}
         */
        public int id           = -1;
        /**
         * Associate with DATA(origin image path) field of the {@link android.provider.MediaStore.Images}
         */
        public String data = "";
        /**
         * Associate with DATE_TAKEN(The take image date) filed of the {@link android.provider.MediaStore.Images}
         */
        public int date_taken    = 0;
        /**
         * Associate with BUCKET_DISPLAY_NAME(The category name) field of the
         * {@link android.provider.MediaStore.Images}
         */
        public String bucket_display_name       = "";
        /**
         * Associate with BUCKET_ID(The category id) field of the
         * {@link android.provider.MediaStore.Images}
         */
        public int bucket_id                    = -1;
        /**
         * Associate with MINI_THUMB_MAGIC(The thumbnails ID) field of the
         * {@link android.provider.MediaStore.Images}
         */
        public int mini_thumb_magic             = -1;
        /**
         * The thumbnails image file path
         */
        public String thumb_path                = "";
        /**
         * This category(BUCKET_ID) contains image items count
         */
        public int item_count                   = 0;

        @Override
        protected Object clone() throws CloneNotSupportedException {
            ItemImage image = new ItemImage();
            image.id = id;
            image.data = data;
            image.date_taken = date_taken;
            image.bucket_display_name = bucket_display_name;
            image.bucket_id = bucket_id;
            image.mini_thumb_magic = mini_thumb_magic;
            image.thumb_path = thumb_path;
            image.item_count = item_count;
            return super.clone();
        }
    }

    /**
     * This method is called from the upload activity and is passed
     * the LinearLayout that should be updated as the directory changes
     * so the user knows which folder they are in.
     *
     * @param pathBar	The label to update as the directory changes
     */
    private void setUpdatePathBar(LinearLayout pathBar) {
        mPathBar = pathBar;
        // Initial path bar default item, Disk, this item is root.
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        TextView textView = (TextView)inflater.inflate(R.layout.path_bar_item_layout, null);
        textView.setTag(0);
        String path = "相册";//mContext.getResources().getString(R.string.upload_activity_bottom_bar_default_item_string);
        textView.setText(path);
        textView.setOnClickListener(mPathBatItemListener);
        mPathBar.addView(textView);
    }

    private void refreshPathBar() {
        int pathBarCount = mPathBar.getChildCount();
        Log.d(TAG, "pathStackCount: " + pathBarCount);

        if (mListMode == CATEGORY_LIST_MODE) {
            if (pathBarCount > 1)
                mPathBar.removeViewAt(pathBarCount - 1);
        } else if (mListMode == ITEM_LIST_MODE) {
            if (pathBarCount == 1) {
                TextView textView = (TextView)mInflater.inflate(R.layout.path_bar_item_layout, null);
                textView.setTag(1);
                String path = "";
                path = mSubItemList.get(0).bucket_display_name;
                Log.d(TAG, "path is " + path);
                textView.setText(path);
                textView.setOnClickListener(mPathBatItemListener);
                mPathBar.addView(textView);
            }
        }
    }

    private void setLoadingViewVisible(boolean visible){
        if(null != mLoadingView && null != mListContainer){
            Log.d(TAG, "Show loading view: " + visible);
            mListContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
            mLoadingView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * This class listening path bar item click event.Path bar's item
     * stand for a folder of current path. When user click one item,
     * the current path should switch to the folder and clear the path
     * bar's extra redundant item.
     */
    private class PathBarItemClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            int index = Integer.valueOf(v.getTag().toString());
            if (index == 0) {
                clearMultiSelect();
                mListMode = CATEGORY_LIST_MODE;
                setListAdapter(mCategoryAdapter);
                mCategoryAdapter.notifyDataSetChanged();
                refreshPathBar();
            }
            //updateContent(mFileMgr.switchToDirByIndex(index));
        }
    }

    private class LoadThumbTask extends AsyncTask<Void, Void, Integer> {

        private int mPosition   = 0;

        private LoadThumbTask(int position) {
            mPosition = position;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // Get image item filtered by bucket_id
            ItemImage category = mCategoryList.get(mPosition);
            mSubItemList.clear();
            for (int i = 0; i < mAllItemList.size(); i++) {
                ItemImage item = mAllItemList.get(i);
                if (category.bucket_id == item.bucket_id) {
                    try {
                        mSubItemList.add((ItemImage) item.clone());
                        getThumbPath(item);
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            setLoadingViewVisible(false);
            setListAdapter(mItemAdapter);
            mItemAdapter.notifyDataSetChanged();
            mListMode = ITEM_LIST_MODE;
            refreshPathBar();
        }
    }
}
