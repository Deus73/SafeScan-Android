package nl.safescan.local;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Build;
import android.provider.OpenableColumns;
import android.provider.Settings;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public String getType(Uri uri){return "application/pdf";}
 public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){File f=new File(getContext().getCacheDir(),"SafeScan-rapport.pdf");String id=Settings.Secure.getString(getContext().getContentResolver(),Settings.Secure.ANDROID_ID);if(id==null||id.length()<6)id="onbekend";else id=id.substring(id.length()-6);String model=(Build.MANUFACTURER+"-"+Build.MODEL).replaceAll("[^A-Za-z0-9_-]+","-");String name="SafeScan-"+model+"-"+id+"-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date())+".pdf";MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});c.addRow(new Object[]{name,f.length()});return c;}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{return ParcelFileDescriptor.open(new File(getContext().getCacheDir(),"SafeScan-rapport.pdf"),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 public int delete(Uri uri,String selection,String[] selectionArgs){return 0;}
 public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs){return 0;}
}
