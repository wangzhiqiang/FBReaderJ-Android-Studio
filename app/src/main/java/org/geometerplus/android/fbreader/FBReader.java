

package org.geometerplus.android.fbreader;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.formatPlugin.PluginUtil;
//import org.geometerplus.android.fbreader.httpd.DataService;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.DictionaryHighlighting;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper;
import org.geometerplus.fbreader.formats.ExternalFormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.error.ErrorKeys;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public final class FBReader extends FBReaderMainActivity implements ZLApplicationWindow {

    public static final int RESULT_DO_NOTHING = RESULT_FIRST_USER;
    public static final int RESULT_REPAINT = RESULT_FIRST_USER + 1;

    public static Intent defaultIntent(Context context) {
        return new Intent(context, FBReader.class)
            .setAction(FBReaderIntents.Action.VIEW)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    public static void openBookActivity(Context context, Book book, Bookmark bookmark) {
        final Intent intent = defaultIntent(context);
        FBReaderIntents.putBookExtra(intent, book);
        FBReaderIntents.putBookmarkExtra(intent, bookmark);
        context.startActivity(intent);
    }

    private FBReaderApp myFBReaderApp;
    private volatile Book myBook;

    private ZLAndroidWidget myMainView;


    volatile boolean IsPaused = false;

    private Intent myOpenBookIntent = null;

//    private final FBReaderApp.Notifier myNotifier = new AppNotifier(this);


    private synchronized void openBook(Intent intent, final Runnable action, boolean force) {
        if (!force && myBook != null) {
            return;
        }

        myBook = FBReaderIntents.getBookExtra(intent, myFBReaderApp.Collection);
        final Bookmark bookmark = FBReaderIntents.getBookmarkExtra(intent);
        if (myBook == null) {
            final Uri data = intent.getData();
            if (data != null) {
                myBook = createBookForFile(ZLFile.createFileByPath(data.getPath()));
            }
        }
        if (myBook != null) {
            ZLFile file = BookUtil.fileByBook(myBook);
            if (!file.exists()) {
                if (file.getPhysicalFile() != null) {
                    file = file.getPhysicalFile();
                }
                UIMessageUtil.showErrorMessage(this, "fileNotFound", file.getPath());
                myBook = null;
            } else {
                NotificationUtil.drop(this, myBook);
            }
        }
        Config.Instance().runOnConnect(new Runnable() {
            @Override
            public void run() {
                myFBReaderApp.openBook(myBook, bookmark, action, null);
                AndroidFontUtil.clearFontCache();
            }
        });
    }

    private Book createBookForFile(ZLFile file) {
        if (file == null) {
            return null;
        }
        Book book = myFBReaderApp.Collection.getBookByFile(file.getPath());
        if (book != null) {
            return book;
        }
        if (file.isArchive()) {
            for (ZLFile child : file.children()) {
                book = myFBReaderApp.Collection.getBookByFile(child.getPath());
                if (book != null) {
                    return book;
                }
            }
        }
        return null;
    }



    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);


        setContentView(R.layout.main);
        myMainView = (ZLAndroidWidget) findViewById(R.id.main_view);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        myFBReaderApp = (FBReaderApp) FBReaderApp.Instance();
        if (myFBReaderApp == null) {
            myFBReaderApp = new FBReaderApp(Paths.systemInfo(this), new BookCollectionShadow());
        }
        getCollection().bindToService(this, null);
        myBook = null;

        myFBReaderApp.setWindow(this);
        myFBReaderApp.initWindow();

        myFBReaderApp.setExternalFileOpener(new ExternalFileOpener(this));


        //打开书库
        myFBReaderApp
            .addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(this, myFBReaderApp));

        //返回键 弹出menu
        myFBReaderApp
            .addAction(ActionCode.SHOW_CANCEL_MENU, new ShowCancelMenuAction(this, myFBReaderApp));

        myOpenBookIntent = getIntent();
//
    }


    @Override
    protected void onNewIntent(final Intent intent) {
        final String action = intent.getAction();

        //选择打开书籍
        if (Intent.ACTION_VIEW.equals(action) || FBReaderIntents.Action.VIEW.equals(action)) {
            myOpenBookIntent = intent;
            if (myFBReaderApp.Model == null && myFBReaderApp.ExternalBook != null) {
                final BookCollectionShadow collection = getCollection();
                final Book b = FBReaderIntents.getBookExtra(intent, collection);
                if (!collection.sameBook(b, myFBReaderApp.ExternalBook)) {
                    try {
                        final ExternalFormatPlugin plugin =
                            (ExternalFormatPlugin) BookUtil.getPlugin(
                                PluginCollection.Instance(Paths.systemInfo(this)),
                                myFBReaderApp.ExternalBook
                            );
                        startActivity(
                            PluginUtil.createIntent(plugin, FBReaderIntents.Action.PLUGIN_KILL));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
//
        else {
            super.onNewIntent(intent);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        IsPaused = false;

         if (myOpenBookIntent != null) {
            final Intent intent = myOpenBookIntent;
            myOpenBookIntent = null;
            getCollection().bindToService(this, new Runnable() {
                @Override
                public void run() {
                    openBook(intent, null, true);
                }
            });
        }
//

    }

    @Override
    protected void onPause() {
        IsPaused = true;
        myFBReaderApp.onWindowClosing();

        super.onPause();
    }


    @Override
    protected void onDestroy() {
        getCollection().unbind();
        super.onDestroy();
    }


    @Override
    public boolean onSearchRequested() {

        return true;
    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
//
            case REQUEST_CANCEL_MENU:
                runCancelAction(data);
                break;
        }
    }

    private void runCancelAction(Intent intent) {
        final CancelMenuHelper.ActionType type;
        try {
            type = CancelMenuHelper.ActionType.valueOf(
                intent.getStringExtra(FBReaderIntents.Key.TYPE)
            );
        } catch (Exception e) {
            // invalid (or null) type value
            return;
        }
        Bookmark bookmark = null;
        if (type == CancelMenuHelper.ActionType.returnTo) {
            bookmark = FBReaderIntents.getBookmarkExtra(intent);
            if (bookmark == null) {
                return;
            }
        }
        myFBReaderApp.runCancelAction(type, bookmark);
    }

    public void navigate() {
        ((NavigationPopup) myFBReaderApp.getPopupById(NavigationPopup.ID)).runNavigation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }




    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (myMainView != null && myMainView.onKeyDown(keyCode, event)) || super
            .onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return (myMainView != null && myMainView.onKeyUp(keyCode, event)) || super
            .onKeyUp(keyCode, event);
    }





    private BookCollectionShadow getCollection() {
        return (BookCollectionShadow) myFBReaderApp.Collection;
    }

    // methods from ZLApplicationWindow interface
    @Override
    public void showErrorMessage(String key) {
        UIMessageUtil.showErrorMessage(this, key);
    }

    @Override
    public void showErrorMessage(String key, String parameter) {
        UIMessageUtil.showErrorMessage(this, key, parameter);
    }

    @Override
    public FBReaderApp.SynchronousExecutor createExecutor(String key) {
        return UIUtil.createExecutor(this, key);
    }

    private int myBatteryLevel;

    @Override
    public int getBatteryLevel() {
        return myBatteryLevel;
    }


    @Override
    public void close() {
        finish();
    }

    @Override
    public ZLViewWidget getViewWidget() {
        return myMainView;
    }

    private final HashMap<MenuItem, String> myMenuItemMap = new HashMap<MenuItem, String>();


    @Override
    public void refresh() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<MenuItem, String> entry : myMenuItemMap.entrySet()) {
                    final String actionId = entry.getValue();
                    final MenuItem menuItem = entry.getKey();
                    menuItem.setVisible(myFBReaderApp.isActionVisible(actionId) && myFBReaderApp
                        .isActionEnabled(actionId));
                    switch (myFBReaderApp.isActionChecked(actionId)) {
                        case TRUE:
                            menuItem.setCheckable(true);
                            menuItem.setChecked(true);
                            break;
                        case FALSE:
                            menuItem.setCheckable(true);
                            menuItem.setChecked(false);
                            break;
                        case UNDEFINED:
                            menuItem.setCheckable(false);
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();

        final Intent intent = new Intent(
            FBReaderIntents.Action.ERROR,
            new Uri.Builder().scheme(exception.getClass().getSimpleName()).build()
        );
        intent.setPackage(FBReaderIntents.DEFAULT_PACKAGE);
        intent.putExtra(ErrorKeys.MESSAGE, exception.getMessage());
        final StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        intent.putExtra(ErrorKeys.STACKTRACE, stackTrace.toString());

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // ignore
            e.printStackTrace();
        }
    }

    @Override
    public void setWindowTitle(final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(title);
            }
        });
    }


    public void outlineRegion(ZLTextRegion.Soul soul) {
        myFBReaderApp.getTextView().outlineRegion(soul);
        myFBReaderApp.getViewWidget().repaint();
    }


    @Override
    public void hideDictionarySelection() {
        myFBReaderApp.getTextView().hideOutline();
        myFBReaderApp.getTextView().removeHighlightings(DictionaryHighlighting.class);
        myFBReaderApp.getViewWidget().reset();
        myFBReaderApp.getViewWidget().repaint();
    }
}
