package co.anybooks.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.zlibrary.ui.android.R;

public class FileListActivity extends AppCompatActivity {


    TextView mTextPath;
    ListView mList;

    FileListAdapter adapter = new FileListAdapter();

    private final BookCollectionShadow myCollection = new BookCollectionShadow();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        mTextPath = findViewById(R.id.path);
        mList = findViewById(R.id.list);

        mList.setAdapter(adapter);

        myCollection.bindToService(this, new Runnable() {
            @Override
            public void run() {



            }
        });

        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FileListAdapter adapter = (FileListAdapter) parent.getAdapter();

                File file = adapter.getItem(position);

                final Book book = myCollection.getBookByFile(file.getAbsolutePath());

                    FBReader.openBookActivity(FileListActivity.this, book, null);

            }
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
        myCollection.unbind();
        super.onDestroy();

    }

    class BookFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {

            return pathname.getName().endsWith(".epub")|| pathname.isDirectory();
        }
    }
}
