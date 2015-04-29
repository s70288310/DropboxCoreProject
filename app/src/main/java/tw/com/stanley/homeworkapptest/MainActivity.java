package tw.com.stanley.homeworkapptest;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.util.ArrayList;
import java.util.List;

import static com.dropbox.client2.DropboxAPI.Entry;


public class MainActivity extends ActionBarActivity implements FileInfoDialogFragment.DialogClickListener {

    final static private String APP_KEY = "nwrsbl2na2oxhom";
    final static private String APP_SECRET = "5mrfskgwen1w0v2";
    private static final String PREF_ACCOUNT_NAME = "pref_account";
    private static final String ACCESS_TOKEN_NAME = "access_token_name";
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private static ArrayList<Entry> list_dir = new ArrayList<>();
    private boolean onTask = false;
    private boolean mLoggedin;

    FileListAdapter fileAdapter;
    ListView lv_filelist;
    HorizontalScrollView hsv_dir;
    ImageButton btn_update;
    Button btn_login;
    LinearLayout dirContainer;
    TextView txv_msg;
    private final String tag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initLog();
        if (mLoggedin) {
            new GetMetadataTask().execute("/");
        } else {
            mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
        }

    }

    private void showFileInfo(Entry entry) {
        Log.d(tag, "showFileInfo");
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment fileinfo = FileInfoDialogFragment.newInstance
                (entry.fileName(), entry.mimeType, entry.size,entry.path);
        fileinfo.show(fm, "dialog_file_nfo");
    }

    private void initLog() {
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI<>(session);
        setLoggin(mDBApi.getSession().isLinked());
    }

    private void initView() {
        fileAdapter = new FileListAdapter(this);
        setContentView(R.layout.layout_filelist);
        txv_msg = (TextView) findViewById(R.id.txv_msg);
        hsv_dir = (HorizontalScrollView) findViewById(R.id.hsv_dir);
        hsv_dir.setHorizontalScrollBarEnabled(false);
        lv_filelist = (ListView) findViewById(R.id.lv_filelist);
        lv_filelist.setAdapter(fileAdapter);
        btn_login = (Button) findViewById(R.id.btn_login);
        btn_update = (ImageButton) findViewById(R.id.btn_update);
        dirContainer = (LinearLayout) findViewById(R.id.dircontainer);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(tag, "btn_update click  list_die: " + list_dir.size());
                updateView();
            }
        });

        lv_filelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Entry entry = list_dir.get(list_dir.size() - 1).contents.get(position);
                if (onTask)
                    Toast.makeText(MainActivity.this, getString(R.string.updating), Toast.LENGTH_SHORT).show();
                else if (entry.isDir) {
                    new GetMetadataTask().execute(entry.path);

                } else {
                    showFileInfo(entry);
                }
            }
        });

        lv_filelist.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Entry entry = list_dir.get(list_dir.size() - 1).contents.get(position);
                if (onTask)
                    Toast.makeText(MainActivity.this, getString(R.string.updating), Toast.LENGTH_SHORT).show();
                else if (entry.isDir) {
                    list_dir.remove(list_dir.size() - 1);
                    new DeleteTask().execute(entry.path);
                }
                return false;
            }
        });


        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoggedin) {
                    loggout();
                } else {
                    mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
                }
            }
        });
    }

    private void updateView() {
        if (onTask)
            Toast.makeText(MainActivity.this, getString(R.string.updating), Toast.LENGTH_SHORT).show();
        else if (list_dir.size() > 0) {
            Entry entry = list_dir.get(list_dir.size() - 1);
            Log.d(tag, "btn_update click  path: " + entry.path);
            list_dir.remove(list_dir.size() - 1);
            new GetMetadataTask().execute(entry.path);
        } else {
            list_dir.clear();
            new GetMetadataTask().execute("/"); //return to root
        }
    }


    private void loggout() {
        mDBApi.getSession().unlink();
        setLoggin(false);
        clearToken();
        list_dir = new ArrayList<>();
        updateDir();
        fileAdapter.setEntryList(new ArrayList<Entry>());
        showMessage(true, getString(R.string.alreadylogout));
    }

    private void showMessage(boolean b, String msg) {
        Log.d(tag, b + "  " + msg);
        txv_msg.setText(msg);
//        if(b)
//            txv_msg.setVisibility(View.GONE);
//        else
//            txv_msg.setVisibility(View.VISIBLE);

    }

    private void clearToken() {
        SharedPreferences prefs = getSharedPreferences(PREF_ACCOUNT_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    private void setLoggin(boolean linked) {
        mLoggedin = linked;
        if (linked) {
            btn_login.setText("Logout");
            btn_update.setClickable(true);
            btn_update.setAlpha(1f);
        } else {
            btn_login.setText("Login");
            btn_update.setClickable(false);
            btn_update.setAlpha(0.5f);
        }
    }

    //建立session
    private AndroidAuthSession buildSession() {
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        loadSession(session);
        return session;
    }

    //讀取token並存於session
    private void loadSession(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(PREF_ACCOUNT_NAME, 0);
        String accessToken = prefs.getString(ACCESS_TOKEN_NAME, null);
        if (accessToken == null)
            return;
        session.setOAuth2AccessToken(accessToken);
    }


    protected void onResume() {
        super.onResume();
        Log.d(tag, "onResume    login : " + mLoggedin);
        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                AndroidAuthSession session = mDBApi.getSession();
                session.finishAuthentication();
                storeToken();
                setLoggin(true);
                updateView();
                showMessage(false, "");
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    private void storeToken() {
        AndroidAuthSession session = mDBApi.getSession();
        String accessToken = session.getOAuth2AccessToken();
        if (accessToken != null) {
            SharedPreferences preferences = getSharedPreferences(PREF_ACCOUNT_NAME, 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ACCESS_TOKEN_NAME, accessToken);
            editor.commit();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        Log.d(tag, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();

//       if(id==R.id.action_login){
//            if (mLoggedin) {
//                loggout();
//                item.setTitle("Log out");
//            } else {
//                mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
//            }
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPositiveClick(DialogFragment dialog) {
        Entry entry = list_dir.get(list_dir.size() - 1);
        List<Entry> list_file = entry.contents;
        String path = dialog.getArguments().getString("PATH");
        for (int i = 0; i < list_file.size(); i++) {
            if (list_file.get(i).path.equals(path))
                list_file.remove(i);
        }
        Log.d(tag, "list_contents : " + entry.contents.size() + "    list_file : " + list_file.size());
        new DeleteTask().execute(path, list_dir.get(list_dir.size() - 1).path);
    }

    @Override
    public void onNegtiveClick(DialogFragment dialog) {

    }

    class DeleteTask extends AsyncTask<String, Boolean, Entry> {
        private final String tag = "DeleteTask";

        @Override
        protected Entry doInBackground(String... params) {
            Log.d(tag, tag + "  delete path: " + params[0]);
            onTask = true;
            if (mDBApi.getSession().isLinked()) {
                try {
                    mDBApi.delete(params[0]);
                    return null;
                } catch (DropboxException e) {
                    Log.d("GetMetadataTask", e.toString());
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onProgressUpdate(Boolean... values) {

        }

        @Override
        protected void onPostExecute(Entry entry) {
            List<Entry> list = list_dir.get(list_dir.size() - 1).contents;
            fileAdapter.setEntryList((ArrayList<Entry>) list);
            if (list.size() == 0)
                showMessage(true, getString(R.string.empty));
            else
                showMessage(false, "");
            updateDir();
            onTask = false;
        }
    }

    class GetMetadataTask extends AsyncTask<String, Boolean, Entry> {

        private final String tag = "GetMetadataTask";
        private String path = "";

        @Override
        protected Entry doInBackground(String... params) {
            Log.d("GetMetadataTask", "GetMetadataTask : " + params[0]);
            onTask = true;
            if (mDBApi.getSession().isLinked()) {
                path = params[0];
                try {
                    return mDBApi.metadata(params[0], 0, null, true, null);
                } catch (DropboxException e) {
                    Log.d("GetMetadataTask", e.toString());
                    e.printStackTrace();
                }

            }
            return null;
        }

        protected void onProgressUpdate(Boolean... values) {
            Log.d(tag, "onProgressUpdate : " + values[0]);
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(Entry entry) {
            if (entry != null) {
//                getEntryInfo(entry);
                List<Entry> list = entry.contents;
                fileAdapter.setEntryList((ArrayList<Entry>) list);
                String parentPath = entry.parentPath();
                if (list_dir.size() == 0) {//無資料路徑則新增
                    Log.d(tag, "first");
                    list_dir.add(entry);
                } else {
                    //依既有路徑更新至path  parent層的下一層
                    for (int i = list_dir.size() - 1; i >= 0; i--) {
                        Log.d(tag, parentPath + " : " + list_dir.get(i).path + "/");
                        if (parentPath.equals(list_dir.get(i).path + "/") || (i == 0 && parentPath.equals("/"))) {
                            list_dir.add(entry);
                            break;
                        } else
                            list_dir.remove(i);
                    }

                }
                if (list.size() == 0)
                    showMessage(true, getString(R.string.empty));
                else
                    showMessage(false, "");
                updateDir();
            } else {
                //找不到資料
                Toast.makeText(MainActivity.this, path + getString(R.string.find_no_path)
                        , Toast.LENGTH_SHORT).show();
                showMessage(true, path + " not found");
                new GetMetadataTask().execute("/");
            }
            onTask = false;
        }

    }

    private void getEntryInfo(String tag, Entry entry) {
        Log.d(tag, "icon : " + entry.icon);
        Log.d(tag, "filename : " + entry.fileName());
        Log.d(tag, "rev : " + entry.rev);
        Log.d(tag, "root : " + entry.root);
        Log.d(tag, "path : " + entry.path);
        Log.d(tag, "parentPath : " + entry.parentPath());
        Log.d(tag, "size : " + entry.size);
        if (entry.isDir) {
            Log.d(tag, "isDir ");
            Log.d(tag, "list size : " + entry.contents.size());
        }
    }

    private void updateDir() {
        dirContainer.removeAllViews();
        for (int i = 0; i < list_dir.size(); i++) {
            final Button dir = new Button(MainActivity.this);
            dir.setBackgroundResource(android.R.color.transparent);
            String filename = list_dir.get(i).fileName();
            Log.d(tag, i + ".  " + filename);
            dir.setText((filename.equals("")) ? "dropbox" : filename);
            dir.setTag(i);
            dir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = (Integer) v.getTag();
                    for (int j = list_dir.size() - 1; j > index; j--) {
                        list_dir.remove(j);
                        dirContainer.removeViewAt(j + j);
                        dirContainer.removeViewAt(j + j - 1);
                    }
                    ArrayList<Entry> list = (ArrayList<Entry>) list_dir.get(index).contents;
                    if (list.size() == 0)
                        showMessage(true, getString(R.string.empty));
                    else
                        showMessage(false, "");
                    fileAdapter.setEntryList(list);

                }
            });
            dirContainer.addView(dir);
            if (i != list_dir.size() - 1) {
                TextView txv = new TextView(MainActivity.this);
                txv.setText(">");
                dirContainer.addView(txv);
                dir.setTextColor(0xFF47525D);
            } else
                dir.setTextColor(0xFF007EE5);

        }
        hsv_dir.post(new Runnable() {
            @Override
            public void run() {
                hsv_dir.fullScroll(ScrollView.FOCUS_RIGHT);
            }
        });

    }
}
