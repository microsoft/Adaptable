/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

public interface BulkUpdatable {
	/**
	 * Allows to defer state update to honor a bulk add.
	 */
	void hintBulkOpBegin();
	/**
	 * Allows to finish state update after a bulk add.
	 */
	void hintBulkOpCompleted();
}
