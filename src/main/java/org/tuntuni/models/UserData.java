/*
 * Copyright 2016 Tuntuni.
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
package org.tuntuni.models;

import java.io.Serializable;
import javafx.scene.image.Image;
import org.tuntuni.util.Commons;

/**
 * Data that is passed between server and client as User Profile
 */
public class UserData implements Serializable {

    private static final int AVATAR_MAX_SIZE = 128;

    private final String mName;
    private final String mStatus;
    private final String mAboutMe;
    private final String mState;
    private final byte[] mAvatar;

    public UserData(UserProfile profile) {
        mName = profile.username();
        mStatus = profile.status();
        mAboutMe = profile.aboutme();
        mState = profile.getState();
        mAvatar = Commons.imageToBytes(
                profile.getAvatarImage(AVATAR_MAX_SIZE, AVATAR_MAX_SIZE));
    }

    public String getUserName() {
        return mName;
    }

    public String getState() {
        return mState;
    }

    public String getStatus() {
        return mStatus;
    }

    public String getAboutMe() {
        return mAboutMe;
    }

    public Image getAvatar() {
        return Commons.bytesToImage(mAvatar);
    }

    public Image getAvatar(double width, double height) {
        return Commons.resizeImage(getAvatar(), width, height);
    }
}
