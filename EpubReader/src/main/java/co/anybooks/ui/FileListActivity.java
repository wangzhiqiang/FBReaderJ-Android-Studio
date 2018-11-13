package co.anybooks.ui;

import android.Manifest;
import android.Manifest.permission;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import co.anybooks.R;
import com.github.dfqin.grantor.PermissionListener;
import com.github.dfqin.grantor.PermissionsUtil;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

public class FileListActivity extends AppCompatActivity {


    TextView mTextPath;
    ListView mList;

    FileListAdapter adapter = new FileListAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        mTextPath = findViewById(R.id.path);
        mList = findViewById(R.id.list);

        mList.setAdapter(adapter);


        mList.setOnItemClickListener((parent, view, position, id) -> {

            FileListAdapter adapter = (FileListAdapter) parent.getAdapter();
            File file = adapter.getItem(position);
            Intent intent = new Intent(FileListActivity.this,BookReadActivity.class);
            intent.putExtra(BookReadActivity.KEY_BOOK_PATH,file.getAbsolutePath());
            startActivity(intent);

        });


        if (PermissionsUtil.hasPermission(this, permission.WRITE_EXTERNAL_STORAGE)) {
            //有访问权限
            updateList();
        } else {
            PermissionsUtil.requestPermission(this, new PermissionListener() {
                @Override
                public void permissionGranted(@NonNull String[] permissions) {
                    //用户授予了访问权限

                    updateList();
                }


                @Override
                public void permissionDenied(@NonNull String[] permissions) {
                    //用户拒绝了访问的申请
                }
            }, permission.WRITE_EXTERNAL_STORAGE);
        }


    }


    private void updateList(){
        File file = new File("/sdcard/aaa");

        if(file.isDirectory()){
            File[] files = file.listFiles(new BookFileFilter());
            adapter.updateAll(Arrays.asList(files));

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    class BookFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {

            return pathname.getName().endsWith(".txt")|| pathname.getName().endsWith(".epub")|| pathname.isDirectory();
        }
    }
}
