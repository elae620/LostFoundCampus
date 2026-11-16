package com.example.lostfoundcampus.ui.matches;

import android.os.Bundle;
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
import com.example.lostfoundcampus.data.model.Match;
import com.example.lostfoundcampus.databinding.FragmentMatchesBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.ui.detail.AnnouncementDetailFragment;
import com.example.lostfoundcampus.ui.map.MapHelper;
import com.example.lostfoundcampus.ui.messages.ChatFragment;
import com.example.lostfoundcampus.util.DateUtil;
import com.example.lostfoundcampus.util.InsetsUtil;

import java.util.ArrayList;
import java.util.List;

/** Correspondances au-dessus du seuil de 60 %. Le score est une indication, jamais une preuve (RG04). */
public class MatchesFragment extends Fragment {

    private static final String ARG_ID = "announcement_id";

    private FragmentMatchesBinding b;

    public static Bundle args(String announcementId) {
        Bundle args = new Bundle();
        args.putString(ARG_ID, announcementId);
        return args;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentMatchesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyPadding(b.scroll, true, true, false);
        b.header.txtTitle.setText(R.string.matches_title);
        b.header.btnBack.setOnClickListener(v -> Nav.back(this));

        FakeRepository repo = FakeRepository.get();
        Announcement mine = repo.announcement(getArguments() == null ? null : getArguments().getString(ARG_ID));
        if (mine == null) {
            Nav.of(this).popBackStack();
            return;
        }

        b.imgMine.setImageResource(mine.coverPhoto());
        b.imgMine.setContentDescription(getString(R.string.cd_announcement_photo, mine.title));
        b.txtMineLabel.setText(getString(R.string.matches_your_announcement, getString(mine.type.labelRes)));
        b.txtMineTitle.setText(mine.title);
        b.txtMineMeta.setText(getString(R.string.separator_dot, MapHelper.label(requireContext(), mine.location),
                DateUtil.shortDate(mine.eventDate)));
        b.cardMine.setOnClickListener(v -> Nav.back(this));

        int threshold = getResources().getInteger(R.integer.lf_match_threshold);
        List<MatchAdapter.Item> items = new ArrayList<>();
        for (Match m : repo.matchesFor(mine.id, threshold)) {
            Announcement other = repo.announcement(m.otherId(mine.id));
            if (other != null) items.add(new MatchAdapter.Item(other, m.score));
        }

        MatchAdapter adapter = new MatchAdapter(new MatchAdapter.Listener() {
            @Override
            public void onView(Announcement a) {
                Nav.go(MatchesFragment.this, R.id.announcementDetailFragment, AnnouncementDetailFragment.args(a.id));
            }

            @Override
            public void onContact(Announcement a) {
                Conversation c = FakeRepository.get().openConversation(a.authorId, a.id);
                Nav.go(MatchesFragment.this, R.id.chatFragment, ChatFragment.args(c.id));
            }
        });
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.list.setAdapter(adapter);
        adapter.submit(items);

        b.txtCount.setText(getResources().getQuantityString(R.plurals.matches_count, items.size(), items.size(), threshold));
        boolean empty = items.isEmpty();
        b.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.txtFooter.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            b.empty.imgEmpty.setImageResource(R.drawable.ic_search);
            b.empty.txtEmptyTitle.setText(R.string.matches_empty_title);
            b.empty.txtEmptyBody.setText(R.string.matches_empty_body);
            b.empty.btnEmptyAction.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
