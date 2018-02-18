//package com.marv42.ebt.newnote.debug;
//
//// http://www.inter-fuser.com/2009/09/live-camera-preview-in-android-emulator.html
//
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URISyntaxException;
//import java.net.URL;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.Rect;
//import android.hardware.Camera;
//import android.hardware.Camera.Size;
//import android.net.Uri;
//import android.util.Log;
//import android.view.SurfaceHolder;
//
//public class SocketCamera {
//
//   private static final String LOG_TAG = "SocketCamera:";
//
//   static private SocketCamera socketCamera;
//   private CameraCapture capture;
//   private Camera parametersCamera;
//   private SurfaceHolder surfaceHolder;
//
//   private final String address = "10.0.2.2";
//
//   private final boolean preserveAspectRatio = true;
//   private final Paint paint = new Paint();
//
//
//   private int width  = 240;
//   private int height = 320;
//   private Rect bounds = new Rect(0, 0, width, height);
//
//   private SocketCamera() {
//      //Just used so that we can pass Camera.Paramters in getters and setters
//      parametersCamera = Camera.open();
//   }
//
//   static public SocketCamera open()
//   {
//      if (socketCamera == null) {
//         socketCamera = new SocketCamera();
//      }
//
//      Log.i(LOG_TAG, "Creating Socket Camera");
//      return socketCamera;
//   }
//
//   public void startPreview(Uri uri) {
//      capture = new CameraCapture();
//      capture.setCapturing(true);
//      capture.setUri(uri);
//      capture.start();
//      Log.i(LOG_TAG, "Starting Socket Camera");
//
//   }
//
//   public void stopPreview(){
//      capture.setCapturing(false);
//      Log.i(LOG_TAG, "Stopping Socket Camera");
//   }
//
//   public void setPreviewDisplay(SurfaceHolder surfaceHolder) throws IOException {
//      this.surfaceHolder = surfaceHolder;
//   }
//
//   public void setParameters(Camera.Parameters parameters) {
//      //Bit of a hack so the interface looks like that of
//      Log.i(LOG_TAG, "Setting Socket Camera parameters");
//      parametersCamera.setParameters(parameters);
//      Size size = parameters.getPreviewSize();
//      bounds = new Rect(0, 0, size.width, size.height);
//   }
//
//   public Camera.Parameters getParameters() {
//      Log.i(LOG_TAG, "Getting Socket Camera parameters");
//      return parametersCamera.getParameters();
//   }
//
//   public void release() {
//      Log.i(LOG_TAG, "Releasing Socket Camera parameters");
//      //TODO need to implement this function
//   }
//
//   public boolean isCapturing() {
//      return capture.isCapturing();
//   }
//
//
//
//   private class CameraCapture extends Thread {
//
//      private boolean capturing = false;
//      private Uri mUri;
//
//      public boolean isCapturing() {
//         return capturing;
//      }
//
//      public void setCapturing(boolean capturing) {
//         this.capturing = capturing;
//      }
//
//      public void setUri(Uri uri) {
//         mUri = uri;
//      }
//
//      @Override
//      public void run() {
//         while (capturing) {
//            Canvas c = null;
//            try {
//               c = surfaceHolder.lockCanvas(null);
//               synchronized (surfaceHolder) {
//                  try {
//
//                     // http://stackoverflow.com/questions/1630258/android-problem-bug-with-threadsafeclientconnmanager-downloading-images
//                     HttpGet httpRequest = null;
//
//                     try {
//                        httpRequest = new HttpGet(new URL("http://" + address).toURI());
//                     } catch (URISyntaxException e) {
//                        e.printStackTrace();
//                     }
//
//                     HttpClient httpclient = new DefaultHttpClient();
//                     HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
//
//                     HttpEntity entity = response.getEntity();
//                     BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
//                     InputStream instream = bufHttpEntity.getContent();
//
//                     Bitmap bitmap = BitmapFactory.decodeStream(instream);
//
//                     //render it to canvas, scaling if necessary
//                     if (
//                           bounds.right == bitmap.getWidth() &&
//                           bounds.bottom == bitmap.getHeight()) {
//                        c.drawBitmap(bitmap, 0, 0, null);
//                     } else {
//                        Rect dest;
//                        if (preserveAspectRatio) {
//                           dest = new Rect(bounds);
//                           dest.bottom = bitmap.getHeight() * bounds.right / bitmap.getWidth();
//                           dest.offset(0, (bounds.bottom - dest.bottom)/2);
//                        } else {
//                           dest = bounds;
//                        }
//                        if (c != null)
//                        {
//                           c.drawBitmap(bitmap, null, dest, paint);
//                        }
//                     }
//
//                     try
//                     {
//                        FileOutputStream out = new FileOutputStream(mUri.toString());
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                     } catch (Exception e) {
//                        e.printStackTrace();
//                     }
//
//                  } catch (RuntimeException e) {
//                     e.printStackTrace();
//
//                  } catch (IOException e) {
//                     e.printStackTrace();
//                  } finally {
//
//                  }
//               }
//            } catch (Exception e) {
//               e.printStackTrace();
//            } finally {
//
//               // do this in a finally so that if an exception is thrown
//               // during the above, we don't leave the Surface in an
//               // inconsistent state
//               if (c != null) {
//                  surfaceHolder.unlockCanvasAndPost(c);
//               }
//            }
//         }
//         Log.i(LOG_TAG, "Socket Camera capture stopped");
//      }
//   }
//}
