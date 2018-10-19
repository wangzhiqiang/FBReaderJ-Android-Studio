package co.anybooks.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import co.anybooks.R;
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

//        TODO load data

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
