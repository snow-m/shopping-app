/*
 * Copyright (C) 2008 ZXing authors
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

package com.huohoubrowser.zxing.client.android.result;


import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.Toast;

import com.huohoubrowser.zxing.Result;
import com.huohoubrowser.zxing.client.android.Contents;
import com.huohoubrowser.zxing.client.result.ParsedResult;
import com.huohoubrowser.zxing.client.result.ParsedResultType;
import com.taobao.R;
import com.taobao.utils.ApplicationUtils;
import com.taobao.utils.Constants;

/**
 * A base class for the Android-specific barcode handlers. These allow the app to polymorphically
 * suggest the appropriate actions for each data type.
 *
 * This class also contains a bunch of utility methods to take common actions like opening a URL.
 * They could easily be moved into a helper object, but it can't be static because the Activity
 * instance is needed to launch an intent.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public abstract class ResultHandler {

  private static final String TAG = ResultHandler.class.getSimpleName();

  private static final String[] EMAIL_TYPE_STRINGS = {"home", "work", "mobile"};
  private static final String[] PHONE_TYPE_STRINGS = {"home", "work", "mobile", "fax", "pager", "main"};
  private static final String[] ADDRESS_TYPE_STRINGS = {"home", "work"};
  private static final int[] EMAIL_TYPE_VALUES = {
      ContactsContract.CommonDataKinds.Email.TYPE_HOME,
      ContactsContract.CommonDataKinds.Email.TYPE_WORK,
      ContactsContract.CommonDataKinds.Email.TYPE_MOBILE,
  };
  private static final int[] PHONE_TYPE_VALUES = {
      ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
      ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
      ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
      ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
      ContactsContract.CommonDataKinds.Phone.TYPE_PAGER,
      ContactsContract.CommonDataKinds.Phone.TYPE_MAIN,
  };
  private static final int[] ADDRESS_TYPE_VALUES = {
      ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
      ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK,
  };
  private static final int NO_TYPE = -1;

  public static final int MAX_BUTTON_COUNT = 4;

  private final ParsedResult result;
  private final Activity activity;
  private final Result rawResult;

  ResultHandler(Activity activity, ParsedResult result) {
    this(activity, result, null);
  }

  ResultHandler(Activity activity, ParsedResult result, Result rawResult) {
    this.result = result;
    this.activity = activity;
    this.rawResult = rawResult;

    // Make sure the Shopper button is hidden by default. Without this, scanning a product followed
    // by a QR Code would leave the button on screen among the QR Code actions.
   // View shopperButton = activity.findViewById(R.id.shopper_button);
   // shopperButton.setVisibility(View.GONE);
  }

  public ParsedResult getResult() {
    return result;
  }

  Activity getActivity() {
    return activity;
  }

  /**
   * Indicates how many buttons the derived class wants shown.
   *
   * @return The integer button count.
   */
  public abstract int getButtonCount();

  /**
   * The text of the nth action button.
   *
   * @param index From 0 to getButtonCount() - 1
   * @return The button text as a resource ID
   */
  public abstract int getButtonText(int index);


  /**
   * Execute the action which corresponds to the nth button.
   *
   * @param index The button that was clicked.
   */
  public abstract void handleButtonPress(int index);

  /**
   * Some barcode contents are considered secure, and should not be saved to history, copied to
   * the clipboard, or otherwise persisted.
   *
   * @return If true, do not create any permanent record of these contents.
   */
  public boolean areContentsSecure() {
    return false;
  }

  /**
   * Create a possibly styled string for the contents of the current barcode.
   *
   * @return The text to be displayed.
   */
  public CharSequence getDisplayContents() {
    String contents = result.getDisplayResult();
    return contents.replace("\r", "");
  }

  /**
   * A string describing the kind of barcode that was found, e.g. "Found contact info".
   *
   * @return The resource ID of the string.
   */
  public abstract int getDisplayTitle();

  /**
   * A convenience method to get the parsed type. Should not be overridden.
   *
   * @return The parsed type, e.g. URI or ISBN
   */
  public final ParsedResultType getType() {
    return result.getType();
  }

  final void addPhoneOnlyContact(String[] phoneNumbers,String[] phoneTypes) {
    addContact(null, null, phoneNumbers, phoneTypes, null, null, null, null, null, null, null, null, null, null);
  }

  final void addEmailOnlyContact(String[] emails, String[] emailTypes) {
    addContact(null, null, null, null, emails, emailTypes, null, null, null, null, null, null, null, null);
  }

  final void addContact(String[] names,
                        String pronunciation,
                        String[] phoneNumbers,
                        String[] phoneTypes,
                        String[] emails,
                        String[] emailTypes,
                        String note,
                        String instantMessenger,
                        String address,
                        String addressType,
                        String org,
                        String title,
                        String url,
                        String birthday) {

    // Only use the first name in the array, if present.
    Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT, ContactsContract.Contacts.CONTENT_URI);
    intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
    putExtra(intent, ContactsContract.Intents.Insert.NAME, names != null ? names[0] : null);

    putExtra(intent, ContactsContract.Intents.Insert.PHONETIC_NAME, pronunciation);

    int phoneCount = Math.min(phoneNumbers != null ? phoneNumbers.length : 0, Contents.PHONE_KEYS.length);
    for (int x = 0; x < phoneCount; x++) {
      putExtra(intent, Contents.PHONE_KEYS[x], phoneNumbers[x]);
      if (phoneTypes != null && x < phoneTypes.length) {
        int type = toPhoneContractType(phoneTypes[x]);
        if (type >= 0) {
          intent.putExtra(Contents.PHONE_TYPE_KEYS[x], type);
        }
      }
    }

    int emailCount = Math.min(emails != null ? emails.length : 0, Contents.EMAIL_KEYS.length);
    for (int x = 0; x < emailCount; x++) {
      putExtra(intent, Contents.EMAIL_KEYS[x], emails[x]);
      if (emailTypes != null && x < emailTypes.length) {
        int type = toEmailContractType(emailTypes[x]);
        if (type >= 0) {
          intent.putExtra(Contents.EMAIL_TYPE_KEYS[x], type);
        }
      }
    }

    // No field for URL, birthday; use notes
    StringBuilder aggregatedNotes = new StringBuilder();
    for (String aNote : new String[] { url, birthday, note }) {
      if (aNote != null) {
        if (aggregatedNotes.length() > 0) {
          aggregatedNotes.append('\n');
        }
        aggregatedNotes.append(aNote);
      }
    }
    if (aggregatedNotes.length() > 0) {
      putExtra(intent, ContactsContract.Intents.Insert.NOTES, aggregatedNotes.toString());
    }
    
    putExtra(intent, ContactsContract.Intents.Insert.IM_HANDLE, instantMessenger);
    putExtra(intent, ContactsContract.Intents.Insert.POSTAL, address);
    if (addressType != null) {
      int type = toAddressContractType(addressType);
      if (type >= 0) {
        intent.putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE, type);
      }
    }
    putExtra(intent, ContactsContract.Intents.Insert.COMPANY, org);
    putExtra(intent, ContactsContract.Intents.Insert.JOB_TITLE, title);
    launchIntent(intent);
  }

  private static int toEmailContractType(String typeString) {
    return doToContractType(typeString, EMAIL_TYPE_STRINGS, EMAIL_TYPE_VALUES);
  }

  private static int toPhoneContractType(String typeString) {
    return doToContractType(typeString, PHONE_TYPE_STRINGS, PHONE_TYPE_VALUES);
  }

  private static int toAddressContractType(String typeString) {
    return doToContractType(typeString, ADDRESS_TYPE_STRINGS, ADDRESS_TYPE_VALUES);
  }

  private static int doToContractType(String typeString, String[] types, int[] values) {
    if (typeString == null) {
      return NO_TYPE;
    }
    for (int i = 0; i < types.length; i++) {
      String type = types[i];
      if (typeString.startsWith(type) || typeString.startsWith(type.toUpperCase(Locale.ENGLISH))) {
        return values[i];
      }
    }
    return NO_TYPE;
  }
  
  final void shareByEmail(String contents) {
    sendEmailFromUri("mailto:", null, activity.getString(R.string.msg_share_subject_line),
        contents);
  }

  final void sendEmail(String address, String subject, String body) {
    sendEmailFromUri("mailto:" + address, address, subject, body);
  }

  // Use public Intent fields rather than private GMail app fields to specify subject and body.
  final void sendEmailFromUri(String uri, String email, String subject, String body) {
    Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(uri));
    if (email != null) {
      intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
    }
    putExtra(intent, Intent.EXTRA_SUBJECT, subject);
    putExtra(intent, Intent.EXTRA_TEXT, body);
    intent.setType("text/plain");
    launchIntent(intent);
  }

  final void shareBySMS(String contents) {
    sendSMSFromUri("smsto:", contents); //activity.getString(R.string.msg_share_subject_line) + ":\n" +
  }
  
  final void copyToClipboard(String contents){
	  ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
	  if (contents != null) {				
		clipboard.setText(contents.toString());
		Toast.makeText(activity, R.string.Commons_CopyToastMessage, Toast.LENGTH_SHORT).show();
	 }
  }

  final void sendSMS(String phoneNumber, String body) {
    sendSMSFromUri("smsto:" + phoneNumber, body);
  }

  final void sendSMSFromUri(String uri, String body) {
    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
    putExtra(intent, "sms_body", body);
    // Exit the app once the SMS is sent
    intent.putExtra("compose_mode", true);
    launchIntent(intent);
  }  

  final void sendMMS(String phoneNumber, String subject, String body) {
    sendMMSFromUri("mmsto:" + phoneNumber, subject, body);
  }

  final void sendMMSFromUri(String uri, String subject, String body) {
    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
    // The Messaging app needs to see a valid subject or else it will treat this an an SMS.
    if (subject == null || subject.length() == 0) {
      putExtra(intent, "subject", activity.getString(R.string.msg_default_mms_subject));
    } else {
      putExtra(intent, "subject", subject);
    }
    putExtra(intent, "sms_body", body);
    intent.putExtra("compose_mode", true);
    launchIntent(intent);
  }

  final void dialPhone(String phoneNumber) {
    launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
  }

  final void dialPhoneFromUri(String uri) {
    launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse(uri)));
  }

  final void openMap(String geoURI) {
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(geoURI)));
  }
  
  final void toSysShare(String body) {
//	  String title=activity.getString(R.string.app);
//	  ApplicationUtils.sharePage(activity, title, body);
  }

  /**
   * Do a geo search using the address as the query.
   *
   * @param address The address to find
   * @param title An optional title, e.g. the name of the business at this address
   */
  final void searchMap(String address, CharSequence title) {
    String query = address;
    if (title != null && title.length() > 0) {
      query += " (" + title + ')';
    }
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))));
  }

  final void getDirections(double latitude, double longitude) {
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com.hk/maps?f=d&daddr=" + latitude + ',' + longitude)));
  }

  // Uses the mobile-specific version of Product Search, which is formatted for small screens.
  final void openProductSearch(String upc) {
    //Uri uri = Uri.parse("http://www.google.com.hk/m/products?q=" + upc + "&source=zxing");
	//launchIntent(new Intent(Intent.ACTION_VIEW, uri));
	launchIntent("http://www.google.com.hk/m/products?q=" + upc + "&source=wisebrowser");    
  }

  final void openBookSearch(String isbn) {
	  launchIntent("http://books.google.com.hk/books?vid=isbn" + isbn);
     // Uri uri = Uri.parse("http://books.google.com.hk/books?vid=isbn" + isbn);
     //launchIntent(new Intent(Intent.ACTION_VIEW, uri));
  }

//  final void searchBookContents(String isbnOrUrl) {
//    Intent intent = new Intent(Intents.SearchBookContents.ACTION);
//    intent.setClassName(activity, SearchBookContentsActivity.class.getName());
//    putExtra(intent, Intents.SearchBookContents.ISBN, isbnOrUrl);
//    launchIntent(intent);
//  }

  final void openURL(String url) {
    // Strangely, some Android browsers don't seem to register to handle HTTP:// or HTTPS://.
    // Lower-case these as it should always be OK to lower-case these schemes.
    if (url.startsWith("HTTP://")) {
      url = "http" + url.substring(4);
    } else if (url.startsWith("HTTPS://")) {
      url = "https" + url.substring(5);
    }
    launchIntent(url);
//    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//    try {
//      launchIntent(intent);
//    } catch (ActivityNotFoundException anfe) {
//      Log.w(TAG, "Nothing available to handle " + intent);
//    }
  }

  final void webSearch(String query) {
    //Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
    // intent.putExtra("query", query);
    launchIntent(query);
  }

  /**
   * Like {@link #launchIntent(Intent)} but will tell you if it is not handle-able
   * via {@link ActivityNotFoundException}.
   *
   * @throws ActivityNotFoundException
   */
  void rawLaunchIntent(Intent intent) {
    if (intent != null) {    	
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      Log.d(TAG, "Launching intent: " + intent + " with extras: " + intent.getExtras());
      activity.startActivity(intent);
    }
  }
  
  void rawLaunchIntent(String url){
//	  Intent i = new Intent();
//	  i.putExtra(Constants.EXTRA_ID_NEW_TAB, true);
//	  i.putExtra(Constants.EXTRA_ID_URL,url);	
//	  activity.setResult(MainActivity.OPEN_CAPTURE_ACTIVITY, i);
//	  activity.finish(); 
  }

  void launchIntent(String url) {
	  try {
	      rawLaunchIntent(url);
	  } catch (Exception e) {
	      e.printStackTrace();
	  }
  }
  
  /**
   * Like {@link #rawLaunchIntent(Intent)} but will show a user dialog if nothing is available to handle.
   */
  void launchIntent(Intent intent) {
    try {
      rawLaunchIntent(intent);
    } catch (ActivityNotFoundException e) {
      AlertDialog.Builder builder = new AlertDialog.Builder(activity);
      builder.setTitle(R.string.app_name);
      builder.setMessage(R.string.msg_intent_failed);
      builder.setPositiveButton(R.string.button_ok, null);
      builder.show();
    }
  }

  private static void putExtra(Intent intent, String key, String value) {
    if (value != null && value.length() > 0) {
      intent.putExtra(key, value);
    }
  }

}
