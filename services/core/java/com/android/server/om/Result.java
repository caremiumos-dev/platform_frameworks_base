/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.om;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;

final class Result {
    public static final int TYPE_ERROR = 0;
    public static final int TYPE_OK = 1;
    public static final int TYPE_OK_WITH_DATA = 2;

    public final int type;
    public final String errorMessage;
    public final String packageName;
    public final @UserIdInt int userId;

    private static final Result OK = new Result(TYPE_OK, null, null, 0);

    private Result(int type, @Nullable String errorMessage, @Nullable String packageName,
            @UserIdInt int userId) {
        this.type = type;
        this.errorMessage = errorMessage;
        this.packageName = packageName;
        this.userId = userId;
    }

    public static Result error(@NonNull String message) {
        return new Result(TYPE_ERROR, message, null, 0);
    }

    public static Result ok() {
        return OK;
    }

    public static Result ok(@NonNull String targetPackageName, int userId) {
        return new Result(TYPE_OK_WITH_DATA, null, targetPackageName, userId);
    }
}
