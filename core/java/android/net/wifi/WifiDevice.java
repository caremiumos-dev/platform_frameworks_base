package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Describes a WiFi peer device (XTC addition).
 *
 * @hide
 */
public class WifiDevice implements Parcelable {
    public static final int DISCONNECTED = 0;
    public static final int CONNECTED = 1;

    public String deviceAddress;
    public String deviceName;
    public int deviceState;

    public WifiDevice() {
        deviceAddress = "";
        deviceName = "";
        deviceState = DISCONNECTED;
    }

    public WifiDevice(String address, boolean connected) {
        deviceAddress = "";
        deviceName = "";
        deviceState = DISCONNECTED;
        if (connected) {
            deviceState = CONNECTED;
        }
        deviceAddress = address;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceAddress);
        dest.writeString(deviceName);
        dest.writeInt(deviceState);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof WifiDevice)) {
            return false;
        }
        WifiDevice other = (WifiDevice) obj;
        if (deviceAddress == null) {
            return other.deviceAddress == null;
        }
        return deviceAddress.equals(other.deviceAddress);
    }

    public static final Parcelable.Creator<WifiDevice> CREATOR =
            new Parcelable.Creator<WifiDevice>() {
        @Override
        public WifiDevice createFromParcel(Parcel in) {
            WifiDevice device = new WifiDevice();
            device.deviceAddress = in.readString();
            device.deviceName = in.readString();
            device.deviceState = in.readInt();
            return device;
        }

        @Override
        public WifiDevice[] newArray(int size) {
            return new WifiDevice[size];
        }
    };
}