package co.anybooks.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import co.anybooks.R;
import co.anybooks.ui.adapter.BookIndexListAdapter;
import org.geometerplus.fbreader.fbreader.FBReaderApp;


/**
 * book index
 */
public class BookIndexActivity extends AppCompatActivity {



    BookIndexListAdapter adapter = new BookIndexListAdapter();
    RecyclerView mRecyclerView  ;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_book_index);


        mRecyclerView = findViewById(R.id.book_index_list);

        mRecyclerView.setAdapter(adapter);

        FBReaderApp fbReader=  (FBReaderApp) FBReaderApp.Instance();


    }
}
