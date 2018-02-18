///*
// * Copyright (C) 2007 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.marv42.ebt.newnote.debug;
//
//import java.io.IOException;
//
//import android.app.Activity;
//import android.hardware.Camera;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.view.View;
//import android.widget.Button;
//
//import com.marv42.ebt.newnote.R;
//
//
//
//public class CameraPreview extends Activity implements SurfaceHolder.Callback
//{
//   private SurfaceHolder mHolder;
//   private SocketCamera  mCamera;
//   private Uri           mUri;
//
//
//
//   @Override
//   protected void
//   onCreate(Bundle savedInstanceState)
//   {
//      super.onCreate(savedInstanceState);
//      setContentView(R.layout.camerapreview);
//
//      final SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
//
//      mHolder = preview.getHolder();
//
//      mUri = (Uri) getIntent().getExtras().get(MediaStore.EXTRA_OUTPUT);
//      mHolder.addCallback(this);
//      mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
//
//      preview.setOnClickListener(
//         new View.OnClickListener()
//      {
//         public void onClick(View v)
//         {
//            if (mCamera.isCapturing())
//               mCamera.stopPreview();
//            else
//               mCamera.startPreview(mUri);
//         }
//      });
//
//      Button pictureButton = (Button) findViewById(R.id.picture_button);
//      pictureButton.setOnClickListener(
//         new View.OnClickListener()
//      {
//         public void onClick(View v)
//         {
//            setResult(Activity.RESULT_OK/*, mUri.toString()*/);
//            finish();
//         }
//      });
//   }
//
//
//
//   public void
//   surfaceCreated(SurfaceHolder holder)
//   {
//      mCamera = SocketCamera.open();
//      try {
//         mCamera.setPreviewDisplay(holder);
//      } catch (IOException exception) {
//         mCamera.release();
//         mCamera = null;
//         // TODO Add more exception handling logic here
//      }
//   }
//
//
//
//   public void
//   surfaceDestroyed(SurfaceHolder holder)
//   {
//      mCamera.stopPreview();
//      mCamera = null;
//   }
//
//
//
//   public void
//   surfaceChanged(SurfaceHolder holder, int format, int w, int h)
//   {
//      Camera.Parameters parameters = mCamera.getParameters();
//      parameters.setPreviewSize(w, h);
//      mCamera.setParameters(parameters);
//      mCamera.startPreview(mUri);
//   }
//}
