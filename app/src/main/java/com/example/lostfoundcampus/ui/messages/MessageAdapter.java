package com.example.lostfoundcampus.ui.messages;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.model.Message;
import com.example.lostfoundcampus.databinding.ItemMessageReceivedBinding;
import com.example.lostfoundcampus.databinding.ItemMessageSentBinding;
import com.example.lostfoundcampus.util.DateUtil;

import java.util.List;

/** Bulles envoyées / reçues. « Envoi en cours… » sur un message en attente, « Envoyé · 10:43 » en fin de série. */
class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 0;
    private static final int TYPE_RECEIVED = 1;

    private final List<Message> messages;
    private final String me;

    MessageAdapter(List<Message> messages, String me) {
        this.messages = messages;
        this.me = me;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).senderId.equals(me) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            return new SentHolder(ItemMessageSentBinding.inflate(inflater, parent, false));
        }
        return new ReceivedHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message m = messages.get(position);
        if (holder instanceof SentHolder) {
            ItemMessageSentBinding b = ((SentHolder) holder).b;
            Context c = b.getRoot().getContext();
            b.txtBubble.setText(m.text);
            boolean sending = m.state == Message.State.SENDING;
            ViewCompat.setBackgroundTintList(b.txtBubble, ColorStateList.valueOf(ContextCompat.getColor(c,
                    sending ? R.color.lf_violet_400 : R.color.lf_violet_600)));
            boolean endOfRun = position == messages.size() - 1
                    || !messages.get(position + 1).senderId.equals(me);
            if (sending) {
                b.txtMeta.setText(R.string.chat_sending);
                b.txtMeta.setVisibility(View.VISIBLE);
            } else if (endOfRun) {
                b.txtMeta.setText(c.getString(R.string.chat_sent_at, DateUtil.time(m.sentAt)));
                b.txtMeta.setVisibility(View.VISIBLE);
            } else {
                b.txtMeta.setVisibility(View.GONE);
            }
        } else {
            ((ReceivedHolder) holder).b.txtBubble.setText(m.text);
        }
    }

    static class SentHolder extends RecyclerView.ViewHolder {
        final ItemMessageSentBinding b;

        SentHolder(ItemMessageSentBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        final ItemMessageReceivedBinding b;

        ReceivedHolder(ItemMessageReceivedBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
