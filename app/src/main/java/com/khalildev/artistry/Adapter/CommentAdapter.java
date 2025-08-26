package com.khalildev.artistry.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khalildev.artistry.Model_Class.CommentItem;
import com.khalildev.artistry.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private List<CommentItem> comments;

    public CommentAdapter(List<CommentItem> comments) {
        this.comments = comments;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentItem c = comments.get(position);
        holder.tvUser.setText(c.getUserName());
        holder.tvText.setText(c.getText());

        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(c.getTimestamp().toDate().getTime());
        String time = DateFormat.getDateTimeInstance().format(cal.getTime());

        holder.tvTime.setText(time);
    }

    @Override public int getItemCount() { return comments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvText, tvTime;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.commentName);
            tvText = itemView.findViewById(R.id.commentText);
            tvTime = itemView.findViewById(R.id.commentTime);
        }
    }
}
