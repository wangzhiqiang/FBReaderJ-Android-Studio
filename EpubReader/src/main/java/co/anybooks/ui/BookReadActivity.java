package co.anybooks.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import co.anybooks.R;
import co.anybooks.ui.action.ShowMenuAction;
import co.anybooks.ui.action.ShowMenuAction.MenuShow;
import java.util.List;
import org.geometerplus.android.fbreader.ProcessHyperlinkAction;
import org.geometerplus.android.fbreader.SelectionShowPanelAction;
import org.geometerplus.android.fbreader.SwitchProfileAction;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.bookmodel.TOCTree.Reference;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

public class BookReadActivity extends AppCompatActivity {

    private String TAG = getClass().getSimpleName();

    public static String KEY_BOOK_PATH = "BOOK_PATH";

//    private BookCollectionShadow mCollection;


    private FBReaderApp myFBReaderApp;
    private volatile Book book;

    private ZLAndroidWidget mContentView;

    private SeekBar seekBar;

    private View v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_read);
//        mCollection = new BookCollectionShadow();

        mContentView = findViewById(R.id.book_read_content);

        v = findViewById(R.id.menu);
        myFBReaderApp = (FBReaderApp) FBReaderApp.Instance();
        if (myFBReaderApp == null) {
            myFBReaderApp = new FBReaderApp(Paths.systemInfo(this));
        }

        myFBReaderApp.setWindow(() -> mContentView);

        myFBReaderApp.initWindow();

        myFBReaderApp.addAction(ActionCode.SHOW_MENU,new ShowMenuAction(show -> {
            myFBReaderApp.getViewWidget().setPopupWindowStatus(show);
           ShowMenuAction action = (ShowMenuAction) myFBReaderApp.getAction(ActionCode.SHOW_MENU);
           action.setShow(show);
            if (show){
                v.setVisibility(View.VISIBLE);
            }else {
                v.setVisibility(View.INVISIBLE);
            }

        },myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL,
            new SelectionShowPanelAction(this, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.PROCESS_HYPERLINK,
            new ProcessHyperlinkAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SWITCH_TO_DAY_PROFILE,
            new SwitchProfileAction(myFBReaderApp, ColorProfile.DAY));
        myFBReaderApp.addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE,
            new SwitchProfileAction(myFBReaderApp, ColorProfile.NIGHT));

        findViewById(R.id.font_to_small).setOnClickListener(
            v -> myFBReaderApp.runAction(ActionCode.DECREASE_FONT));

        findViewById(R.id.font_to_big).setOnClickListener(
            v -> myFBReaderApp.runAction(ActionCode.INCREASE_FONT));

        findViewById(R.id.change_day_night).setOnClickListener(v -> {

            //  切换 白天/夜间 模式
            String vla = myFBReaderApp.ViewOptions.ColorProfileName.getValue();

            if (ColorProfile.NIGHT.equals(vla)) {
                myFBReaderApp.runAction(ActionCode.SWITCH_TO_DAY_PROFILE);
            } else {
                myFBReaderApp.runAction(ActionCode.SWITCH_TO_NIGHT_PROFILE);
            }

        });

        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(
            new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


                    saveLocalData(progress);


                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        findViewById(R.id.show_book_index).setOnClickListener(v -> {
            //TODO 显示目录

            TOCTree tree = myFBReaderApp.Model.TOCTree;

            printTOC(tree);
        });
    }


    private void printTOC(TOCTree tocTree) {

        Reference reference = tocTree.getReference();

        Log.i(TAG, "printTOC: " + tocTree.getText() + " PageIndex:" + (reference != null
            ? reference.ParagraphIndex : ""));
        List<TOCTree> sub = tocTree.subtrees();
        if (sub != null && !sub.isEmpty()) {

            for (TOCTree t : sub) {
                printTOC(t);
            }

        }
    }


    @Override
    protected void onDestroy() {
        myFBReaderApp.removeAction(ActionCode.SELECTION_SHOW_PANEL);
        super.onDestroy();
    }


    private boolean isLocalBrightness() {


        return getLocalData() > 0;
    }

    private int getLocalData() {
        SharedPreferences sp = getSharedPreferences("bbb", MODE_PRIVATE);
        return sp.getInt("b", -1);
    }

    private void saveLocalData(int d) {
        SharedPreferences sp = getSharedPreferences("bbb", MODE_PRIVATE);
        sp.edit().putInt("b", d).apply();
        BrightnessUtils.changeAppBrightness(BookReadActivity.this,d);
    }

    private int b = 0;
    private boolean isAutoModel = false;


    @Override
    protected void onResume() {
        super.onResume();

        //读取本地配置 没有就不管

        if (isLocalBrightness()) {

            isAutoModel = BrightnessUtils.isAutoBrightness(this);

            int p = getLocalData();
            b = BrightnessUtils.getSystemBrightness(this);

            BrightnessUtils.changeAppBrightness(this, p);

            Log.i(TAG, "onResume: "+b);

            seekBar.setProgress(p);
        }
        openBook();
        Window window = getWindow();
        Rect rect= new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);

    }


    @Override
    protected void onPause() {

        if (isLocalBrightness()) {
            //还原设置

            BrightnessUtils.changeAppBrightness(this,b);

        }
        super.onPause();

    }

    private void openBook() {

        Intent intent = getIntent();

        String bookPath = intent.getStringExtra(KEY_BOOK_PATH);

        book = new Book(0, bookPath, "xx", "utf-8", "en");



        myFBReaderApp.openBook(book, null, null);
//        Log.i(TAG, "openBook: -----");
//      getPagePosition(()->{
          Log.i(TAG, "Reader pos: "+myFBReaderApp.getTextView().pagePosition());
//      });


    }

    int count =10;
    public void getPagePosition(Runnable promise) {

        try {
            ZLTextView view = myFBReaderApp.getTextView();
            ZLTextView.PagePosition pagePosition = view.pagePosition();

            //处理page第一次获取为3的问题
            if (pagePosition.Total <= 3 && --count >= 0) {
                new Handler().postDelayed(() -> getPagePosition(promise), 50);
                return;
            }

            promise.run();

        } catch (Exception e) {

        }
        count = 10;

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                myFBReaderApp.runAction(ActionCode.TURN_PAGE_FORWARD);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                myFBReaderApp.runAction(ActionCode.TURN_PAGE_BACK);
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }


}
