package co.anybooks.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import co.anybooks.ui.adapter.BookIndexListAdapter.BookIndexItemHolder;

public class BookIndexListAdapter extends RecyclerView.Adapter<BookIndexItemHolder> {

    @NonNull
    @Override
    public BookIndexItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull BookIndexItemHolder viewHolder, int i) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }



    static class BookIndexItemHolder  extends ViewHolder{



        public BookIndexItemHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
