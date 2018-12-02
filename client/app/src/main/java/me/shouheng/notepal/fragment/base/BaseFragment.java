package me.shouheng.notepal.fragment.base;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.databinding.ViewDataBinding;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.webkit.WebView;

import java.io.File;

import me.shouheng.commons.activity.PermissionActivity;
import me.shouheng.commons.fragment.CommonFragment;
import me.shouheng.commons.utils.PermissionUtils;
import me.shouheng.commons.utils.ToastUtils;
import me.shouheng.data.entity.Attachment;
import me.shouheng.notepal.R;
import me.shouheng.notepal.manager.FileManager;
import me.shouheng.notepal.util.AttachmentHelper;
import me.shouheng.notepal.util.ScreenShotHelper;
import me.shouheng.notepal.util.tools.Callback;
import me.shouheng.notepal.util.tools.Invoker;
import me.shouheng.notepal.util.tools.Message;

/**
 * Base fragment, used to handle the shared and common logic.
 *
 * Created by WngShhng (shouheng2015@gmail.com) on 2017/12/29.
 * Refactored by WngShhng (shouheng2015@gmail.com) on 2017/12/01. */
public abstract class BaseFragment<V extends ViewDataBinding> extends CommonFragment<V>
        implements AttachmentHelper.OnAttachingFileListener {

    // region Screen capture region

    /**
     * Screen capture method, used to capture the screen for RecyclerView.
     * The {@link #createScreenCapture(RecyclerView, int)} used to capture the list with a
     * fixed list item height. Override the method {@link #onGetScreenCaptureFile(File)} to handle
     * the captured image file.
     *
     * @param recyclerView the recycler view to capture
     */
    protected void createScreenCapture(final RecyclerView recyclerView) {
        if (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0) {
            ToastUtils.makeToast(R.string.text_empty_list);
            return;
        }
        if (getActivity() == null) return;
        PermissionUtils.checkStoragePermission((PermissionActivity) getActivity(), () -> doCapture(recyclerView, 0));
    }

    protected void createScreenCapture(final RecyclerView recyclerView, final int itemHeight) {
        if (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0) {
            ToastUtils.makeToast(R.string.text_empty_list);
            return;
        }
        if (getActivity() == null) return;
        PermissionUtils.checkStoragePermission((PermissionActivity) getActivity(), () -> doCapture(recyclerView, itemHeight));
    }

    /**
     * Do the recycler view capture logic in this method. The method will use the fixed height
     * screen capture method when the item height is not 0.
     *
     * @param recyclerView the recycler view
     * @param itemHeight item height, will use the fixed height capture method when it's not 0.
     */
    private void doCapture(RecyclerView recyclerView, int itemHeight) {
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setTitle(R.string.text_capturing);
        new Invoker<>(new Callback<File>() {
            @Override
            public void onBefore() {
                pd.setCancelable(false);
                pd.show();
            }

            @Override
            public Message<File> onRun() {
                Message<File> message = new Message<>();
               Bitmap bitmap;
                if (itemHeight == 0) {
                    bitmap = ScreenShotHelper.shotRecyclerView(recyclerView);
                } else {
                    bitmap = ScreenShotHelper.shotRecyclerView(recyclerView, itemHeight);
                }
                boolean succeed = FileManager.saveImageToGallery(getContext(), bitmap, true, message::setObj);
                message.setSucceed(succeed);
                return message;
            }

            @Override
            public void onAfter(Message<File> message) {
                pd.dismiss();
                if (message.isSucceed()) {
                    ToastUtils.makeToast(String.format(getString(R.string.text_file_saved_to), message.getObj().getPath()));
                    onGetScreenCaptureFile(message.getObj());
                } else {
                    ToastUtils.makeToast(R.string.text_failed_to_save_file);
                }
            }
        }).start();
    }

    /**
     * Called when got the captured image file.
     *
     * @param file the captured image file.
     */
    protected void onGetScreenCaptureFile(File file) { }

    /**
     * Capture the WebView.
     *
     * @param webView the WebView to capture.
     * @param listener the image save callback.
     */
    protected void createWebCapture(WebView webView, FileManager.OnSavedToGalleryListener listener) {
        assert getActivity() != null;
        PermissionUtils.checkStoragePermission((PermissionActivity) getActivity(), () -> {
            final ProgressDialog pd = new ProgressDialog(getContext());
            pd.setTitle(R.string.text_capturing);
            pd.setCancelable(false);
            pd.show();

            new Handler().postDelayed(() -> doCapture(webView, pd, listener), 500);
        });
    }

    /**
     * Finally do the screen capture logic.
     *
     * @param webView the WebView to capture
     * @param pd the progress dialog
     * @param listener the callback
     */
    private void doCapture(WebView webView, ProgressDialog pd, FileManager.OnSavedToGalleryListener listener) {
        ScreenShotHelper.shotWebView(webView, listener);
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }

    // endregion screen capture region

    // region Attachment handler region

    /**
     * This method will called when the attachment is sure usable. For the check logic, you may refer
     * to {@link BaseFragment#onAttachingFileFinished(Attachment)}
     *
     * @param attachment the usable attachment */
    protected void onGetAttachment(@NonNull Attachment attachment) {}

    protected void onFailedGetAttachment(Attachment attachment) {}

    @Override
    public void onAttachingFileErrorOccurred(Attachment attachment) {
        onFailedGetAttachment(attachment);
    }

    @Override
    public void onAttachingFileFinished(Attachment attachment) {
        if (AttachmentHelper.checkAttachment(attachment)) {
            onGetAttachment(attachment);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            AttachmentHelper.resolveResult(this, requestCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // endregion attachment handler region

}
