package com.microsoft.authenticate;

import static com.microsoft.authenticate.OAuth.DisplayType;

/**
 * The type of the device is used to determine the display query parameter for login.live.com.
 * Phones have a display parameter of android_phone.
 * Tablets have a display parameter of android_tablet.
 */
enum DeviceType {
    PHONE {
        @Override
        public DisplayType getDisplayParameter() {
            return DisplayType.ANDROID_PHONE;
        }
    },
    TABLET {
        @Override
        public DisplayType getDisplayParameter() {
            return DisplayType.ANDROID_TABLET;
        }
    };

    abstract public DisplayType getDisplayParameter();
}
