package com.crazymen.freenote.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;

import com.crazymen.freenote.MainActivity;
import com.crazymen.freenote.R;
import com.crazymen.freenote.data.NoteStore;
import com.crazymen.freenote.model.Note;
import com.crazymen.freenote.util.AvatarStore;
import com.crazymen.freenote.util.Dates;
import com.crazymen.freenote.util.PhotoStore;
import com.crazymen.freenote.util.Prefs;

import java.util.List;

/** 01 今日：打开即写 */
public class TodayFragment extends Fragment {

    private TextView tvDate;
    private TextView tvGreeting;
    private TextView tvTodayStat;
    private TextView tvEmpty;
    private EditText etQuick;
    private LinearLayout listToday;
    private ImageView avatar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvDate = view.findViewById(R.id.tvDate);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvTodayStat = view.findViewById(R.id.tvTodayStat);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        etQuick = view.findViewById(R.id.etQuick);
        listToday = view.findViewById(R.id.listToday);
        avatar = view.findViewById(R.id.avatar);

        view.findViewById(R.id.btnRecord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureQuick();
            }
        });

        view.findViewById(R.id.btnCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditorActivity.open(requireActivity(), 0L, true, false);
            }
        });

        view.findViewById(R.id.btnTag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditorActivity.open(requireActivity(), 0L, false, true);
            }
        });

        view.findViewById(R.id.avatar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).selectTab(MainActivity.TAB_PROFILE);
                }
            }
        });

        etQuick.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    captureQuick();
                    return true;
                }
                return false;
            }
        });

        view.findViewById(R.id.captureCard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etQuick.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etQuick, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        render();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            render();
        }
    }

    private void captureQuick() {
        String text = etQuick.getText().toString().trim();
        if (text.length() == 0) {
            EditorActivity.open(requireActivity(), 0L, false, false);
            return;
        }
        NoteStore.get(requireContext()).create(text);
        etQuick.setText("");
        etQuick.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etQuick.getWindowToken(), 0);
        }
        render();
    }

    private void render() {
        if (!isAdded() || getView() == null) {
            return;
        }
        Context c = requireContext();
        NoteStore store = NoteStore.get(c);

        tvDate.setText(Dates.dayLabel(System.currentTimeMillis()));
        tvGreeting.setText(String.format("%s，%s", Dates.greeting(), Prefs.name(c)));
        AvatarStore.apply(c, avatar, dp(46));

        long now = System.currentTimeMillis();
        List<Note> notes = store.byDay(now);
        int words = 0;
        for (Note n : notes) {
            words += n.wordCount();
        }
        tvTodayStat.setText(Dates.monthCountText(notes.size(), words));

        listToday.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(c);
        for (Note n : notes) {
            listToday.addView(bindNote(inflater, n));
        }
        tvEmpty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private View bindNote(LayoutInflater inflater, final Note note) {
        View v = inflater.inflate(R.layout.item_note, listToday, false);
        ((TextView) v.findViewById(R.id.tvTime)).setText(Dates.time(note.createdAt));

        TextView chip = v.findViewById(R.id.tvChip);
        String chipText = null;
        boolean moodChip = false;
        if (note.mood != null && note.mood.trim().length() > 0) {
            chipText = note.mood;
            moodChip = true;
        } else if (!note.tags.isEmpty()) {
            chipText = note.tags.get(0);
        }
        if (chipText == null) {
            chip.setVisibility(View.GONE);
        } else {
            chip.setText(chipText);
            chip.setBackgroundResource(moodChip ? R.drawable.bg_chip_primary : R.drawable.bg_chip_sand);
            chip.setTextColor(ContextCompat.getColor(requireContext(),
                    moodChip ? R.color.primary : R.color.text_secondary));
        }

        TextView body = v.findViewById(R.id.tvBody);
        String text = note.body == null ? "" : note.body.trim();
        if (text.length() == 0) {
            text = note.displayTitle();
        }
        body.setText(text);

        ImageView photo = v.findViewById(R.id.ivPhoto);
        String cover = note.coverPhoto();
        Bitmap coverBmp = cover == null ? null : PhotoStore.load(requireContext(), cover, 512);
        if (coverBmp == null) {
            photo.setVisibility(View.GONE);
            photo.setOnClickListener(null);
        } else {
            photo.setVisibility(View.VISIBLE);
            RoundedBitmapDrawable rounded =
                    RoundedBitmapDrawableFactory.create(getResources(), coverBmp);
            rounded.setCornerRadius(dp(12));
            rounded.setAntiAlias(true);
            photo.setImageDrawable(rounded);
            // 点封面直接全屏看，不打断「看列表」这件事
            photo.setClickable(true);
            photo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v2) {
                    PhotoViewerActivity.open(requireActivity(), note.photos, 0);
                }
            });
        }

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v2) {
                NoteDetailActivity.open(requireActivity(), note.id);
            }
        });
        return v;
    }
}
