package com.usharik.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.usharik.app.R;
import com.usharik.app.DeclensionQuizState.WordTextModel;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the draggable word pool in DeclensionQuizFragment.
 *
 * Only visible words are shown. Each item's tag is set to the word's original
 * index in the ViewModel's wordTextModels array so that the Fragment's
 * drag-and-drop handlers can identify which word is being dragged.
 *
 * Identification contract (shared with DeclensionQuizFragment):
 *   - Word pool item tag: plain integer string, e.g. "3"  → no underscore
 *   - Case cell tag:      "numberCode_caseNum", e.g. "0_2" → contains underscore
 */
public class WordDragAdapter extends RecyclerView.Adapter<WordDragAdapter.WordViewHolder> {

    private WordTextModel[] allItems = new WordTextModel[0];
    private final List<Integer> visibleIndices = new ArrayList<>();
    private final View.OnTouchListener touchListener;

    public WordDragAdapter(@NonNull View.OnTouchListener touchListener) {
        this.touchListener = touchListener;
    }

    /**
     * Replace the full word list. Called when the ViewModel's wordTextModels property changes.
     */
    public void updateItems(@NonNull WordTextModel[] items) {
        allItems = items;
        rebuildVisibleIndices();
        notifyDataSetChanged();
    }

    private void rebuildVisibleIndices() {
        visibleIndices.clear();
        for (int i = 0; i < allItems.length; i++) {
            WordTextModel model = allItems[i];
            if (model != null && model.getVisible() == View.VISIBLE) {
                visibleIndices.add(i);
            }
        }
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MaterialTextView view = (MaterialTextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word_drag, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        int wordIndex = visibleIndices.get(position);
        WordTextModel model = allItems[wordIndex];

        // Defensive fallback: adapter can briefly receive placeholder arrays with null items.
        if (model == null) {
            holder.wordView.setText("");
            holder.wordView.setTag("-1");
            holder.wordView.setOnTouchListener(null);
            return;
        }

        holder.wordView.setText(model.getWord());
        // Tag = original index in wordTextModels — used by drag-and-drop in DeclensionQuizFragment.
        holder.wordView.setTag(String.valueOf(wordIndex));
        holder.wordView.setOnTouchListener(touchListener);
    }

    @Override
    public int getItemCount() {
        return visibleIndices.size();
    }

    public static class WordViewHolder extends RecyclerView.ViewHolder {
        private final MaterialTextView wordView;

        WordViewHolder(@NonNull MaterialTextView itemView) {
            super(itemView);
            this.wordView = itemView;
        }
    }
}
