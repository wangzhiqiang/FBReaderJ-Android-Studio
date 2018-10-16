package co.anybooks.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geometerplus.zlibrary.ui.android.R;

public class FileListAdapter extends BaseAdapter {

    private Map<String,Long> mHistory = new HashMap<>();
    private List<File> files = new ArrayList<>();




    public void updateAll(List<File> data){
        files.clear();
        files.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public File getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        H holder;
        if(null == convertView){
            convertView = View.inflate(parent.getContext(),R.layout.activity_file_list_item,null);
            holder = new H();
            holder.imageView = convertView.findViewById(R.id.item_img);
            holder.textView  = convertView.findViewById(R.id.item_text);
            convertView.setTag(holder);
        }else {
            holder = (H) convertView.getTag();
        }

        File item = getItem(position);
        holder.textView.setText( item.getName());
        if(item.isDirectory()){
            holder.imageView.setImageResource(R.drawable.ic_list_library_folder);
        }else {
            holder.imageView.setImageResource(R.drawable.ic_list_library_book);
        }






        return convertView;
    }

    static class H {

        ImageView imageView;
        TextView textView;


    }
}
