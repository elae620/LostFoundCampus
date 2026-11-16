package com.example.lostfoundcampus.ui.messages;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.Conversation;
import com.example.lostfoundcampus.data.model.Message;
import com.example.lostfoundcampus.data.model.User;
import com.example.lostfoundcampus.databinding.ItemConversationBinding;
import com.example.lostfoundcampus.ui.common.UiUtils;
import com.example.lostfoundcampus.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.Holder> {

    interface OnClick {
        void onConversation(Conversation c);
    }

    private final List<Conversation> items = new ArrayList<>();
    private final OnClick onClick;

    ConversationAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    void submit(List<Conversation> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemConversationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Conversation conv = items.get(position);
        Context c = h.b.getRoot().getContext();
        FakeRepository repo = FakeRepository.get();
        String me = repo.currentUser().uid;
        User other = repo.user(conv.otherParticipant(me));
        Announcement linked = repo.announcement(conv.relatedAnnouncementId);
        Message last = conv.lastMessage();
        boolean unread = conv.hasUnreadFor(me);

        if (other != null) {
            UiUtils.bindInitialsAvatar(h.b.txtAvatar, other);
            h.b.txtName.setText(other.fullName);
        }
        h.b.txtName.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
        h.b.txtTime.setText(last == null ? "" : DateUtil.conversationTime(c, last.sentAt));
        h.b.txtPreview.setText(last == null ? c.getString(R.string.messages_no_message_yet) : last.text);
        h.b.unreadDot.setVisibility(unread ? View.VISIBLE : View.INVISIBLE);
        h.b.txtLinked.setText(linked == null ? "" : linked.title);
        h.b.txtLinked.setVisibility(linked == null ? View.GONE : View.VISIBLE);

        // Une seule annonce vocale claire pour la ligne entière.
        StringBuilder cd = new StringBuilder();
        if (other != null) cd.append(other.fullName).append(". ");
        if (unread) cd.append(c.getString(R.string.cd_unread)).append(". ");
        cd.append(h.b.txtPreview.getText());
        if (linked != null) cd.append(". ").append(linked.title);
        h.b.getRoot().setContentDescription(cd);
        h.b.getRoot().setOnClickListener(v -> onClick.onConversation(conv));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemConversationBinding b;

        Holder(ItemConversationBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
