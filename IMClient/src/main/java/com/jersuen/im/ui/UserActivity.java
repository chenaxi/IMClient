package com.jersuen.im.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import com.jersuen.im.IM;
import com.jersuen.im.R;
import com.jersuen.im.ui.view.RoundedImageView;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.io.File;

public class UserActivity extends Activity implements View.OnClickListener {
    /**
     * 选择照片返回码
     */
    private static final int selectCode = 123;

    /**
     * 拍照返回码
     */
    private static final int cameraCode = 124;
    /**
     * 系统裁剪返回码
     */
    private static final int picCode = 125;

    // 拍照文件
    private File tempFile;
    public static final String EXTRA_ID = "account";
    private RoundedImageView avatar;
    private String account;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_user);
        avatar = (RoundedImageView) findViewById(R.id.activity_user_account_avatar);
        account = getIntent().getStringExtra(EXTRA_ID);
        if (!TextUtils.isEmpty(account)) {
            if (account.equals(IM.getString(IM.ACCOUNT_JID))) {
                avatar.setOnClickListener(this);
                findViewById(R.id.activity_user_account_layout).setOnClickListener(this);
                avatar.setImageDrawable(IM.getAvatar(StringUtils.parseName(account)));
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_user_account_avatar:
            case R.id.activity_user_account_layout:
                new AlertDialog.Builder(this)
                        .setTitle("选择照片")
                        .setItems(R.array.select_photo_items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        tempFile = IM.getCameraFile();
                                        // 进入拍照
                                        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
                                        startActivityForResult(intentCamera, cameraCode);
                                        break;
                                    case 1:
                                        // 浏览图库
                                        Intent intentSelect = new Intent();
                                        intentSelect.setType("image/*");
                                        intentSelect.setAction(Intent.ACTION_GET_CONTENT);
                                        startActivityForResult(intentSelect, selectCode);
                                        break;
                                }
                            }
                        }).create().show();
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            // 拍照
            case cameraCode:
                // 获取照片,开始裁剪
                IM.doCropPhoto(UserActivity.this,Uri.fromFile(tempFile),picCode);
                break;
            // 图库
            case selectCode:
                Uri uri = data.getData();
                String[] pojo = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(uri, pojo, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        String pathStr = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        if (!TextUtils.isEmpty(pathStr)) {
                            // 文件后缀判断
                            if (pathStr.endsWith("jpg") || pathStr.endsWith("png")) {
                                // 获取照片,开始裁剪
                                IM.doCropPhoto(UserActivity.this, uri, picCode);
                            }
                        }
                    }
                }
                break;
            // 裁剪
            case picCode:
                if (data != null) {
                    Bitmap photoPic = data.getParcelableExtra("data");
                    if (photoPic != null) {
                        String encodedImage = StringUtils.encodeBase64(IM.Bitmap2Bytes(photoPic));
                        VCard me = new VCard();
                        try {
                            me.load(null);
                            me.setAvatar(IM.Bitmap2Bytes(photoPic),encodedImage);
                            me.save(null);
                        } catch (SmackException.NoResponseException e) {
                            e.printStackTrace();
                        } catch (XMPPException.XMPPErrorException e) {
                            e.printStackTrace();
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
        }
    }
}
