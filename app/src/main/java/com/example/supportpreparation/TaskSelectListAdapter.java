package com.example.supportpreparation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/*
 *
 */
public class TaskSelectListAdapter extends BaseAdapter {

    private LayoutInflater  inflater;
    private int             layoutID;
    List<TaskTable>         tasks;
    //private Context context;

    static class ViewHolder {
        TextView taskName;
        TextView taskTime;
    }

    public TaskSelectListAdapter(Context context, int itemLayoutId, List<TaskTable> tasks) {
        //this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.layoutID = itemLayoutId;
        this.tasks    = tasks;
    }
    @Override
    public int getCount() {
        return tasks.size();
    }
    @Override
    public Object getItem(int position) {
        return position;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }

    // getViewメソッドをOverride
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        // レイアウトを作成
        if (convertView == null) {
            //表示するレイアウトビューを取得
            convertView = inflater.inflate(layoutID, null);

            //
            holder          = new ViewHolder();
            holder.taskName = convertView.findViewById(R.id.tv_taskName);
            holder.taskTime = convertView.findViewById(R.id.tv_taskTime);
            convertView.setTag(holder);

        }else{
            //あれば、再利用
            holder = (ViewHolder) convertView.getTag();
        }

        //値を設定
        holder.taskName.setText( this.tasks.get(position).getTaskName() );
        holder.taskTime.setText(this.tasks.get(position).getTaskTime());

        //表示するビューを返す
        return convertView;
    }

}
