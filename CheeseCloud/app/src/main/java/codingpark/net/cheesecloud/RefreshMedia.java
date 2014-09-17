/*
 * when a file is created,modify or delete,it will used this class to notify the MediaScanner to refresh the media database
 */

package codingpark.net.cheesecloud;

import java.io.File;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;


public class RefreshMedia
{
    private Context mContext;

    static final String EXTERNAL_VOLUME = "external";

    private static final String TAG = "RefreshMedia";

    public RefreshMedia(Context c)
    {
        this.mContext = c;
    }

    public void fileScan(String file){
        File f = new File(file);
        Uri mUri = Uri.fromFile(f);
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mUri));
    }

    public void folderScan(String path){
        File file = new File(path);

        if(file.isDirectory()){
            File[] array = file.listFiles();

            for(int i=0;i<array.length;i++){
                File f = array[i];

                if(f.isFile()){//FILE TYPE
                    fileScan(f.getAbsolutePath());
                }
                else {//FOLDER TYPE
                    folderScan(f.getAbsolutePath());
                }
            }
        }
    }

    public void notifyMediaAdd(String file)
    {
        File mfile = new File(file);
        if(mfile.exists() && !mfile.isDirectory())
        {
					/*
					 * notify the media to scan 
					 */
            fileScan(file);

        }else if(mfile.exists() && mfile.isDirectory()){
            folderScan(file);
        }
    }

    public void notifyMediaDelete(String file)
    {
        final int ID_AUDIO_COLUMN_INDEX = 0;
        final int PATH_AUDIO_COLUMN_INDEX = 1;
        String[] PROJECTION = new String[] {
                Audio.Media._ID,
                Audio.Media.DATA,
        };
        Uri[] mediatypes = new Uri[] {
                Audio.Media.getContentUri(EXTERNAL_VOLUME),
                Video.Media.getContentUri(EXTERNAL_VOLUME),
                Images.Media.getContentUri(EXTERNAL_VOLUME),
        };
        ContentResolver cr = mContext.getContentResolver();
        Cursor c = null;

        for( int i = 0; i < mediatypes.length; i++)
        {
            c = cr.query(mediatypes[i], PROJECTION, null, null, null);
            if(c != null)
            {
                try
                {
                    while(c.moveToNext())
                    {
                        long rowId = c.getLong(ID_AUDIO_COLUMN_INDEX);
                        String path = c.getString(PATH_AUDIO_COLUMN_INDEX);

                        if(path.startsWith(file))
                        {
                            Log.d(TAG, "delete row " + rowId + "in table " + mediatypes[i]);
                            cr.delete(ContentUris.withAppendedId(mediatypes[i], rowId), null, null);
                        }
                    }
                }
                finally
                {
                    c.close();
                    c = null;
                }
            }
        }
    }

}