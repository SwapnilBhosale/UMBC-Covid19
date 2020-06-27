/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package edu.umbc.covid19.main.model;

import org.dpppt.android.sdk.TracingStatus;
import org.dpppt.android.sdk.internal.database.models.ExposureDay;

import java.util.List;

public interface TracingStatusInterface {

	void setStatus(TracingStatus status);

	boolean isReportedAsInfected();

	List<ExposureDay> getExposureDays();

	boolean wasContactReportedAsExposed();

	TracingState getTracingState();

	NotificationState getNotificationState();

	TracingStatus.ErrorState getTracingErrorState();

	TracingStatus.ErrorState getReportErrorState();

	long getDaysSinceExposure();

}
