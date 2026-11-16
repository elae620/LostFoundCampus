package com.example.lostfoundcampus.ui.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.Conversation;
import com.example.lostfoundcampus.databinding.FragmentMessagesBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.util.InsetsUtil;

import java.util.List;

public class MessagesFragment extends Fragment {

    private FragmentMessagesBinding b;
    private ConversationAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentMessagesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyTop(b.top);
        adapter = new ConversationAdapter(c -> Nav.go(this, R.id.chatFragment, ChatFragment.args(c.id)));
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        b.list.setLayoutManager(lm);
        b.list.addItemDecoration(new DividerItemDecoration(requireContext(), lm.getOrientation()));
        b.list.setAdapter(adapter);
        b.etSearch.addTextChangedListener(new Nav.AfterTextChanged(s -> refresh()));

        b.empty.btnEmptyAction.setText(R.string.messages_empty_action);
        b.empty.btnEmptyAction.setOnClickListener(v -> Nav.main(this).selectTab(R.id.homeFragment));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (b == null) return;
        String query = Nav.text(b.etSearch);
        List<Conversation> data = FakeRepository.get().conversations(query);
        adapter.submit(data);
        boolean empty = data.isEmpty();
        b.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            boolean searching = !query.isEmpty();
            b.empty.imgEmpty.setImageResource(searching ? R.drawable.ic_search_off : R.drawable.ic_chat);
            b.empty.txtEmptyTitle.setText(searching ? R.string.home_empty_title : R.string.messages_empty_title);
            b.empty.txtEmptyBody.setText(R.string.messages_empty_body);
            b.empty.btnEmptyAction.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
