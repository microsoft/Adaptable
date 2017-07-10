/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.model;

/**
 * Methods specific to RecyclerView.Adapter
 */
public interface RecyclerAdapter {
	void notifyItemChanged(int position);
	void notifyItemInserted(int position);
	void notifyItemRemoved(int position);
	void notifyItemMoved(int fromPosition, int toPosition);
	void notifyItemRangeChanged(int positionStart, int itemCount);
	void notifyItemRangeInserted(int positionStart, int itemCount);
	void notifyItemRangeRemoved(int positionStart, int itemCount);
}
