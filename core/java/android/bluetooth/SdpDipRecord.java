package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Bluetooth SDP Device ID Profile record (XTC/QTI addition).
 *
 * @hide
 */
public class SdpDipRecord implements Parcelable {
    private final int mSpecificationId;
    private final int mVendorId;
    private final int mVendorIdSource;
    private final int mProductId;
    private final int mVersion;
    private final boolean mPrimaryRecord;
    private final String mClientExecutableUrl;
    private final String mServiceDescription;
    private final String mDocumentationUrl;

    public SdpDipRecord(int specificationId, int vendorId, int vendorIdSource, int productId,
            int version, boolean primaryRecord, String clientExecutableUrl,
            String serviceDescription, String documentationUrl) {
        mSpecificationId = specificationId;
        mVendorId = vendorId;
        mVendorIdSource = vendorIdSource;
        mProductId = productId;
        mVersion = version;
        mPrimaryRecord = primaryRecord;
        mClientExecutableUrl = clientExecutableUrl;
        mServiceDescription = serviceDescription;
        mDocumentationUrl = documentationUrl;
    }

    public SdpDipRecord(Parcel in) {
        mSpecificationId = in.readInt();
        mVendorId = in.readInt();
        mVendorIdSource = in.readInt();
        mProductId = in.readInt();
        mVersion = in.readInt();
        mPrimaryRecord = in.readBoolean();
        mClientExecutableUrl = in.readString();
        mServiceDescription = in.readString();
        mDocumentationUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getClientExecutableUrl() {
        return mClientExecutableUrl;
    }

    public String getDocumentationUrl() {
        return mDocumentationUrl;
    }

    public boolean getPrimaryRecord() {
        return mPrimaryRecord;
    }

    public int getProductId() {
        return mProductId;
    }

    public String getServiceDescription() {
        return mServiceDescription;
    }

    public int getSpecificationId() {
        return mSpecificationId;
    }

    public int getVendorId() {
        return mVendorId;
    }

    public int getVendorIdSource() {
        return mVendorIdSource;
    }

    public int getVersion() {
        return mVersion;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSpecificationId);
        dest.writeInt(mVendorId);
        dest.writeInt(mVendorIdSource);
        dest.writeInt(mProductId);
        dest.writeInt(mVersion);
        dest.writeBoolean(mPrimaryRecord);
        dest.writeString(mClientExecutableUrl);
        dest.writeString(mServiceDescription);
        dest.writeString(mDocumentationUrl);
    }

    public static final Parcelable.Creator<SdpDipRecord> CREATOR =
            new Parcelable.Creator<SdpDipRecord>() {
        @Override
        public SdpDipRecord createFromParcel(Parcel in) {
            return new SdpDipRecord(in);
        }

        @Override
        public SdpDipRecord[] newArray(int size) {
            return new SdpDipRecord[size];
        }
    };
}