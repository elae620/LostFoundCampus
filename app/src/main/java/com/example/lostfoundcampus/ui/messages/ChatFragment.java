package com.example.lostfoundcampus.ui.messages;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.Conversation;
import com.example.lostfoundcampus.data.model.Message;
import com.example.lostfoundcampus.data.model.User;
import com.example.lostfoundcampus.databinding.FragmentChatBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.ui.common.UiUtils;
import com.example.lostfoundcampus.ui.detail.AnnouncementDetailFragment;
import com.example.lostfoundcampus.util.InsetsUtil;

/**
 * Conversation liée à une annonce. Au début d'une conversation vide, un message
 * d'introduction est proposé en brouillon : il n'est jamais envoyé sans action de l'utilisateur.
 */
public class ChatFragment extends Fragment {

    private static final String ARG_ID = "conversation_id";
    private static final String STATE_DRAFT_SHOWN = "draft_shown";

    private FragmentChatBinding b;
    @Nullable
    private Conversation conversation;
    private MessageAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean draftShown;
    private boolean fillingDraft;

    public static Bundle args(String conversationId) {
        Bundle args = new Bundle();
        args.putString(ARG_ID, conversationId);
        return args;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) draftShown = savedInstanceState.getBoolean(STATE_DRAFT_SHOWN);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentChatBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyTop(b.header);
        InsetsUtil.applyBottom(b.inputBar, true);
        b.btnBack.setOnClickListener(v -> Nav.back(this));

        FakeRepository repo = FakeRepository.get();
        conversation = repo.conversation(getArguments() == null ? null : getArguments().getString(ARG_ID));
        if (conversation == null) {
            Nav.of(this).popBackStack();
            return;
        }
        String me = repo.currentUser().uid;
        User other = repo.user(conversation.otherParticipant(me));
        Announcement linked = repo.announcement(conversation.relatedAnnouncementId);
        repo.markRead(conversation.id);

        if (other != null) {
            UiUtils.bindInitialsAvatar(b.txtAvatar, other);
            b.txtName.setText(other.fullName);
        }

        if (linked != null) {
            b.cardLinked.setVisibility(View.VISIBLE);
            b.imgLinked.setImageResource(linked.coverPhoto());
            b.txtLinkedLabel.setText(getString(R.string.chat_linked, getString(linked.type.labelRes)));
            b.txtLinkedTitle.setText(linked.title);
            b.cardLinked.setOnClickListener(v -> Nav.go(this, R.id.announcementDetailFragment,
                    AnnouncementDetailFragment.args(linked.id)));
        } else {
            b.cardLinked.setVisibility(View.GONE);
        }

        adapter = new MessageAdapter(conversation.messages, me);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        b.list.setLayoutManager(lm);
        b.list.setAdapter(adapter);
        renderEmpty();
        scrollToEnd();

        // Brouillon IA : seulement pour une conversation vide, une seule fois.
        if (conversation.messages.isEmpty() && !draftShown && linked != null && other != null) {
            draftShown = true;
            fillingDraft = true;
            b.etMessage.setText(IntroMessageTemplate.build(requireContext(), linked, other));
            fillingDraft = false;
            b.groupDraft.setVisibility(View.VISIBLE);
        }
        b.chipDraft.setCloseIconContentDescription(getString(R.string.cd_clear_draft));
        b.chipDraft.setOnCloseIconClickListener(v -> {
            b.etMessage.setText("");
            b.groupDraft.setVisibility(View.GONE);
        });

        b.etMessage.addTextChangedListener(new Nav.AfterTextChanged(s -> {
            if (fillingDraft) return;
            if (s.toString().trim().isEmpty()) b.groupDraft.setVisibility(View.GONE);
            updateSendButton();
        }));
        updateSendButton();
        b.btnSend.setOnClickListener(v -> send());

        resolvePendingMessages();
    }

    private void updateSendButton() {
        b.btnSend.setEnabled(!Nav.text(b.etMessage).isEmpty());
    }

    private void send() {
        if (conversation == null) return;
        String text = Nav.text(b.etMessage);
        if (text.isEmpty()) return;
        // ÉTAPE 1 : envoi simulé. Étape 2 : écriture Firestore + écoute temps réel.
        Message m = FakeRepository.get().sendMessage(conversation.id, text);
        int index = conversation.messages.size() - 1;
        adapter.notifyItemInserted(index);
        if (index > 0) adapter.notifyItemChanged(index - 1);
        b.etMessage.setText("");
        b.groupDraft.setVisibility(View.GONE);
        renderEmpty();
        scrollToEnd();
        handler.postDelayed(() -> markSent(m), getResources().getInteger(R.integer.lf_fake_send_delay_ms));
    }

    /** Les messages restés « en cours d'envoi » passent à « envoyé » après un court délai (simulation). */
    private void resolvePendingMessages() {
        if (conversation == null) return;
        for (Message m : conversation.messages) {
            if (m.state == Message.State.SENDING) {
                handler.postDelayed(() -> markSent(m), getResources().getInteger(R.integer.lf_fake_send_delay_ms));
            }
        }
    }

    private void markSent(Message m) {
        if (b == null || conversation == null) return;
        m.state = Message.State.SENT;
        int index = conversation.messages.indexOf(m);
        if (index >= 0) adapter.notifyItemChanged(index);
    }

    private void renderEmpty() {
        boolean empty = conversation == null || conversation.messages.isEmpty();
        b.txtEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void scrollToEnd() {
        if (conversation == null || conversation.messages.isEmpty()) return;
        b.list.post(() -> {
            if (b != null && conversation != null) b.list.scrollToPosition(conversation.messages.size() - 1);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_DRAFT_SHOWN, draftShown);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        b = null;
    }
}
