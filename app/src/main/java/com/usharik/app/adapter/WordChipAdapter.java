package com.usharik.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.usharik.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the word-selection chip list in WordsWithErrorsFragment.
 * Replaces the legacy manual RadioButton inflation inside a custom FlowLayout.
 *
 * Selection is single-choice; the selected word is tracked internally and exposed
 * via {@link OnWordSelectedListener} so the Fragment can forward it to the ViewModel.
 */
public class WordChipAdapter extends RecyclerView.Adapter<WordChipAdapter.ChipViewHolder> {

    public interface OnWordSelectedListener {
        void onWordSelected(String word);
    }

    private final List<String> words = new ArrayList<>();
    private String selectedWord;
    private final OnWordSelectedListener listener;

    public WordChipAdapter(String selectedWord, OnWordSelectedListener listener) {
        this.selectedWord = selectedWord;
        this.listener = listener;
    }

    /** Replace the full word list and refresh the selection. */
    public void setWords(List<String> newWords, String currentSelectedWord) {
        words.clear();
        words.addAll(newWords);
        selectedWord = currentSelectedWord;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Chip chip = (Chip) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word_chip, parent, false);
        return new ChipViewHolder(chip);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        String word = words.get(position);
        holder.chip.setText(word);
        // Set checked state without triggering the listener
        holder.chip.setOnCheckedChangeListener(null);
        holder.chip.setChecked(word.equals(selectedWord));
        holder.chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String previous = selectedWord;
                selectedWord = word;
                int prevIndex = words.indexOf(previous);
                if (prevIndex >= 0 && prevIndex != holder.getBindingAdapterPosition()) {
                    notifyItemChanged(prevIndex);
                }
                if (listener != null) {
                    listener.onWordSelected(word);
                }
            } else if (word.equals(selectedWord)) {
                // Prevent deselecting the current chip without another selection
                buttonView.setChecked(true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    public static class ChipViewHolder extends RecyclerView.ViewHolder {
        private final Chip chip;

        ChipViewHolder(@NonNull Chip chip) {
            super(chip);
            this.chip = chip;
        }
    }
}

