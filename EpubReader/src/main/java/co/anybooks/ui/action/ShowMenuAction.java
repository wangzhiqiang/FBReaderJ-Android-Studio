package co.anybooks.ui.action;

import android.util.Log;
import android.view.View;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class ShowMenuAction extends FBAction {

    private boolean show = false;
    private MenuShow menuShow;

    public ShowMenuAction(MenuShow menuShow,
        FBReaderApp fbreader) {
        super(fbreader);
        this.menuShow = menuShow;
    }

    private boolean lastSendStatus = false;




    @Override
    protected void run(Object... params) {

        if (params.length > 0) {
            show = (boolean) params[0];
        }

        Log.i(TAG, "run: " + show + " " + lastSendStatus);
        if (lastSendStatus == show & !lastSendStatus) {
            return;
        }



        menuShow.showNav(!lastSendStatus);
//        WritableMap d = Arguments.createMap();
//        d.putBoolean("show",!lastSendStatus);
//
//
//        emit(ActionCode.SHOW_MENU,d);
    }

    public void setShow(boolean show) {
        this.lastSendStatus = show;

        Log.i(TAG, "setShow: " + show + " l " + lastSendStatus);
    }

   public interface  MenuShow{
        void showNav(boolean show);
    }
}
