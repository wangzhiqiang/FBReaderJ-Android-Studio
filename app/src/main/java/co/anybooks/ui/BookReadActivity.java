package co.anybooks.ui;

import android.content.Intent;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import org.geometerplus.android.fbreader.SelectionShowPanelAction;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.core.application.ZLApplication.SynchronousExecutor;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

public class BookReadActivity extends AppCompatActivity {

    private String TAG = getClass().getSimpleName();

    public static String KEY_BOOK_PATH = "BOOK_PATH";

//    private BookCollectionShadow mCollection;


    private FBReaderApp myFBReaderApp;
    private volatile Book book;

    private ZLAndroidWidget mContentView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_read);

//        mCollection = new BookCollectionShadow();

        mContentView = findViewById(R.id.book_read_content);

        myFBReaderApp = (FBReaderApp) FBReaderApp.Instance();
        if (myFBReaderApp == null) {
            myFBReaderApp = new FBReaderApp(Paths.systemInfo(this));
        }

        myFBReaderApp.setWindow(new ZLApplicationWindow() {
            @Override
            public void setWindowTitle(String title) {

            }

            @Override
            public void showErrorMessage(String resourceKey) {

            }

            @Override
            public void showErrorMessage(String resourceKey, String parameter) {

            }

            @Override
            public SynchronousExecutor createExecutor(String key) {
                return null;
            }

            @Override
            public void processException(Exception e) {

            }

            @Override
            public void refresh() {

            }

            @Override
            public ZLViewWidget getViewWidget() {
                return mContentView;
            }

            @Override
            public void close() {

            }

            @Override
            public int getBatteryLevel() {
                return 0;
            }
        });

        myFBReaderApp.initWindow();

        myFBReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL,new SelectionShowPanelAction(this,myFBReaderApp));
//        mCollection.bindToService(this, new Runnable() {
//            @Override
//            public void run() {
//
//                Log.i(TAG, "Collection.bindToService");
//            }
//        });
    }

    @Override
    protected void onDestroy() {
//        mCollection.unbind();
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();

        try {
            openBook();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void openBook() throws RemoteException {

        Intent intent = getIntent();

        String bookPath = intent.getStringExtra(KEY_BOOK_PATH);

//        bookPath = intent.getExtras().getString(KEY_BOOK_PATH);

        book = new Book(0, bookPath, "xx", "utf-8", "en");


        myFBReaderApp.openBook(book, null, null);

    }

}
