/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package android.telephony.ims.internal;

import android.annotation.SystemApi;
import android.os.RemoteException;
import android.telephony.ims.internal.feature.MmTelFeature;
import android.util.Log;

/**
 * Base implementation for SMS over IMS.
 *
 * Any service wishing to provide SMS over IMS should extend this class and implement all methods
 * that the service supports.
 * @hide
 */
public class SmsImplBase {
  private static final String LOG_TAG = "SmsImplBase";

  /**
   * SMS over IMS format is 3gpp.
   */
  public static final int IMS_SMS_FORMAT_3GPP = 1;

  /**
   * SMS over IMS format is 3gpp2.
   */
  public static final int IMS_SMS_FORMAT_3GPP2 = 2;

  /**
   * Message was sent successfully.
   */
  public static final int SEND_STATUS_OK = 1;

  /**
   * IMS provider failed to send the message and platform should not retry falling back to sending
   * the message using the radio.
   */
  public static final int SEND_STATUS_ERROR = 2;

  /**
   * IMS provider failed to send the message and platform should retry again after setting TP-RD bit
   * to high.
   */
  public static final int SEND_STATUS_ERROR_RETRY = 3;

  /**
   * IMS provider failed to send the message and platform should retry falling back to sending
   * the message using the radio.
   */
  public static final int SEND_STATUS_ERROR_FALLBACK = 4;

  /**
   * Message was delivered successfully.
   */
  public static final int DELIVER_STATUS_OK = 1;

  /**
   * Message was not delivered.
   */
  public static final int DELIVER_STATUS_ERROR = 2;

  /**
   * Status Report was set successfully.
   */
  public static final int STATUS_REPORT_STATUS_OK = 1;

  /**
   * Error while setting status report.
   */
  public static final int STATUS_REPORT_STATUS_ERROR = 2;


  // Lock for feature synchronization
  private final Object mLock = new Object();
  private final int mSmsFormat;
  private IImsSmsListener mListener;

  /**
   * Constructor that defaults to using {@linhk #IMS_SMS_FORMAT_3GPP}.
   */
  public SmsImplBase() {
    this(IMS_SMS_FORMAT_3GPP);
  }

  /**
   * Constructor.
   *
   * @param smsFormat sms format.
   */
  public SmsImplBase(int smsFormat) {
    mSmsFormat = smsFormat;
  }

  /**
   * Registers a listener responsible for handling tasks like delivering messages.
   *
   * @param listener listener to register.
   *
   * @hide
   */
  @SystemApi
  public void registerSmsListener(IImsSmsListener listener) {
    synchronized (mLock) {
      mListener = listener;
    }
  }

  /**
   * This method will be triggered by the platform when the user attempts to send an SMS. This
   * method should be implemented by the IMS providers to provide implementation of sending an SMS
   * over IMS.
   *
   * @param format the format of the message. One of {@link #IMS_SMS_FORMAT_3GPP} or
   *                {@link #IMS_SMS_FORMAT_3GPP2}
   * @param messageRef the message reference.
   * @param isRetry whether it is a retry of an already attempted message or not.
   * @param pdu PDUs representing the contents of the message.
   */
  public void sendSms(int format, int messageRef, boolean isRetry, byte[] pdu) {
    onSendSmsResult(messageRef, SEND_STATUS_ERROR);
  }

  /**
   * This method will be triggered by the platform after {@link #onSmsReceived(int, byte[])} has
   * been called to deliver the result to the IMS provider. It will also be triggered after
   * {@link #onSendSmsResult(int, int)} has been called to provide the result of the operation.
   *
   * @param result Should be {@link #DELIVER_STATUS_OK} if the message was delivered successfully,
   * {@link #DELIVER_STATUS_ERROR} otherwise.
   * @param messageRef the message reference or -1 of unavailable.
   */
  public void acknowledgeSms(int messageRef, int result) {

  }

  /**
   * This method should be triggered by the IMS providers when there is an incoming message. The
   * platform will deliver the message to the messages database and notify the IMS provider of the
   * result by calling {@link #acknowledgeSms(int, int)}.
   *
   * This method must not be called before {@link MmTelFeature#onFeatureReady()} is called.
   *
   * @param format the format of the message.One of {@link #IMS_SMS_FORMAT_3GPP} or
   *                {@link #IMS_SMS_FORMAT_3GPP2}
   * @param pdu PDUs representing the contents of the message.
   * @throws IllegalStateException if called before {@link MmTelFeature#onFeatureReady()}
   */
  public final void onSmsReceived(int format, byte[] pdu) throws IllegalStateException {
    synchronized (mLock) {
      if (mListener == null) {
        throw new IllegalStateException("Feature not ready.");
      }
      try {
        mListener.onSmsReceived(format, pdu);
        acknowledgeSms(-1, DELIVER_STATUS_OK);
      } catch (RemoteException e) {
        Log.e(LOG_TAG, "Can not deliver sms: " + e.getMessage());
        acknowledgeSms(-1, DELIVER_STATUS_ERROR);
      }
    }
  }

  /**
   * This method should be triggered by the IMS providers to pass the result of the sent message
   * to the platform.
   *
   * This method must not be called before {@link MmTelFeature#onFeatureReady()} is called.
   *
   * @param messageRef the message reference.
   * @param result One of {@link #SEND_STATUS_OK}, {@link #SEND_STATUS_ERROR},
   *                {@link #SEND_STATUS_ERROR_RETRY}, {@link #SEND_STATUS_ERROR_FALLBACK}
   * @throws IllegalStateException if called before {@link MmTelFeature#onFeatureReady()}
   * @throws RemoteException
   */
  public final void onSendSmsResult(int messageRef, int result) throws IllegalStateException {
    synchronized (mLock) {
      if (mListener == null) {
        throw new IllegalStateException("Feature not ready.");
      }
      mListener.onSendSmsResult(messageRef, result);
    }
  }

  /**
   * Sets the status report of the sent message.
   *
   * @param messageRef the message reference.
   * @param format Should be {@link #IMS_SMS_FORMAT_3GPP} or {@link #IMS_SMS_FORMAT_3GPP2}
   * @param pdu PDUs representing the content of the status report.
   * @throws IllegalStateException if called before {@link MmTelFeature#onFeatureReady()}
   */
  public final void onSmsStatusReportReceived(int messageRef, int format, byte[] pdu) {
    synchronized (mLock) {
      if (mListener == null) {
        throw new IllegalStateException("Feature not ready.");
      }
      try {
        mListener.onSmsStatusReportReceived(messageRef, format, pdu);
        acknowledgeSms(messageRef, STATUS_REPORT_STATUS_OK);
      } catch (RemoteException e) {
        Log.e(LOG_TAG, "Can not process sms status report: " + e.getMessage());
        acknowledgeSms(messageRef, STATUS_REPORT_STATUS_ERROR);
      }
    }
  }

  /**
   * Returns the SMS format. Default is {@link #IMS_SMS_FORMAT_3GPP} unless overridden by IMS
   * Provider.
   *
   * @return sms format.
   */
  public final int getSmsFormat() {
    return mSmsFormat;
  }

}
