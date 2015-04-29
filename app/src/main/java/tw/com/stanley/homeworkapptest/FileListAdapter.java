package tw.com.stanley.homeworkapptest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;

import java.util.ArrayList;

/**
 * Created by stanley on 2015/4/15.
 */
public class FileListAdapter extends BaseAdapter{


    private ArrayList<DropboxAPI.Entry> entryList=new ArrayList<>();
    private Context context;
    private final String tag="FileListAdapter";

    public FileListAdapter(Context context){
        this.context=context;
    }

    public void setEntryList(ArrayList<DropboxAPI.Entry> entryList) {
        this.entryList = entryList;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return entryList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    class ViewHolder{
        ImageView iv_icon;
        TextView txv_name;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        Log.d(tag,"getView position: "+position);
        ViewHolder holder=new ViewHolder();
        if(convertView==null){
            LayoutInflater inflater= LayoutInflater.from(context);
            convertView=inflater.inflate(R.layout.list_item_file,parent,false);
            holder.iv_icon=(ImageView)convertView.findViewById(R.id.iv_icon);
            holder.txv_name=(TextView)convertView.findViewById(R.id.txv_filename);
            convertView.setTag(holder);
        }else
            holder=(ViewHolder)convertView.getTag();
        holder.txv_name.setText(entryList.get(position).fileName());
        String icon=entryList.get(position).icon;
        String mime=entryList.get(position).mimeType;
        //文件
        int id=R.mipmap.icon_page_white;
        //資料夾
        if(icon.startsWith("folder"))
            id=R.mipmap.icon_folder;
        //壓縮檔
        else if(icon.equals("page_white_compressed"))
            id=R.mipmap.icon_compressed;
        //影音
        else if(mime.startsWith("video") || mime.startsWith("audio"))
            id=R.mipmap.icon_viedo;
        //圖檔
        else if(icon.equals("ai") || mime.startsWith("image"))
            id=R.mipmap.icon_image;
        holder.iv_icon.setImageResource(id);
        return convertView;
    }
}
