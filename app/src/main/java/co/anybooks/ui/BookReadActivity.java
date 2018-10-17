package co.anybooks.ui;

import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import java.util.Locale;
import org.geometerplus.android.fbreader.*;
import org.geometerplus.android.fbreader.api.FBReaderIntents.Action;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
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

    private TextToSpeech textToSpeech; // TTS对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_read);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.CHINA);
                if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
                }
            }

        });




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

        myFBReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL,
            new SelectionShowPanelAction(this, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.PROCESS_HYPERLINK,
            new ProcessHyperlinkAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SWITCH_TO_DAY_PROFILE, new SwitchProfileAction(  myFBReaderApp, ColorProfile.DAY));
        myFBReaderApp.addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, new SwitchProfileAction( myFBReaderApp, ColorProfile.NIGHT));


        findViewById(R.id.font_to_small).setOnClickListener(
            v -> myFBReaderApp.runAction(ActionCode.DECREASE_FONT));

        findViewById(R.id.font_to_big).setOnClickListener(
            v-> myFBReaderApp.runAction(ActionCode.INCREASE_FONT));


        findViewById(R.id.change_day_night).setOnClickListener(v->{

            //  切换 白天/夜间 模式
            String vla = myFBReaderApp.ViewOptions.ColorProfileName.getValue();
            Log.i(TAG, "onCreate: "+vla);
            if(ColorProfile.NIGHT.equals(vla)){
                myFBReaderApp.runAction(ActionCode.SWITCH_TO_DAY_PROFILE);
            }else {
                myFBReaderApp.runAction(ActionCode.SWITCH_TO_NIGHT_PROFILE);
            }

        });
    }

    @Override
    protected void onDestroy() {
        myFBReaderApp.removeAction(ActionCode.SELECTION_SHOW_PANEL);
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();

        openBook();

    }

    private void openBook() {

        Intent intent = getIntent();

        String bookPath = intent.getStringExtra(KEY_BOOK_PATH);

        book = new Book(0, bookPath, "xx", "utf-8", "en");

        myFBReaderApp.openBook(book, null, null);

    }

}
