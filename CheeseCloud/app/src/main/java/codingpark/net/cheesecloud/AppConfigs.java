package codingpark.net.cheesecloud;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ethanshan on 14-9-25.
 */
public class AppConfigs {
    // Application settings info, stored in SharedPreferences
    /**
     * The SharedPreferences name
     */
    public static final String PREFS_NAME              = "CheeseCloudPreferences";	//app preference file name

    /**
     * Set hidden the system files(.profile/.app etc)
     * Data type: boolean
     */
    public static final String PREFS_HIDDEN             = "hidden";
    public static final boolean HIDDEDN_ENABLED         = true;
    public static final boolean HIDDEN_DISABLED         = false;

    /**
     * Set create pictures thumbnails
     * Data type: boolean
     */
    public static final String PREFS_THUMBNAIL          = "thumbnail";
    public static final boolean THUMBNAIL_ENABLED       = true;
    public static final boolean THUMBNAIL_DISABLED      = false;

    /**
     * Set file list sort mode
     * Data type: int
     */
    public static final String PREFS_SORT              = "sort_mode";
    /**
     * No sort
     */
    public static final int SORT_NONE                   = 0;
    /**
     * Sort by alpha of file name
     */
    public static final int SORT_ALPHA                  = 1;
    /**
     * Sort by file type
     */
    public static final int SORT_TYPE                   = 2;

    /**
     * Set login username
     */
    public static final String USERNAME                 = "username";
    /**
     * Set login password(encrypt by MD5)
     */
    public static final String PASSWORD_MD5             = "password_md5";

    /**
     * Set the web services url
     */
    public static final String SERVER_ADDRESS           = "server_address";

    public static long current_local_user_id            = -1;

    public static String current_remote_user_id         = "";
}
