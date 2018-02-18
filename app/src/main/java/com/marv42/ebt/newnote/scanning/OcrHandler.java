/*******************************************************************************
 * Copyright (c) 2010 marvin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     marvin - initial API and implementation
 ******************************************************************************/

package com.marv42.ebt.newnote.scanning;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.R;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TARGET;


// TODO Check if we can continue on certain exceptions
public class OcrHandler extends AsyncTask<Uri, Void, String>
{
   private static final String OCR_HOST = "http://svc.webservius.com/v1/wisetrend/wiseocr/submit";

   private static final int RESULT_POLLING_INTERVAL = 5000; // ms
   public static final int OCR_NOTIFICATION_ID = 2;

   private EbtNewNote mEbtObject;

   public OcrHandler(final EbtNewNote ebtObject)
   {
      mEbtObject = ebtObject;
   }

   @Override
   protected String doInBackground(Uri... params)
   {
      return doOcr(params[0]);
   }

   @Override
   protected void onPostExecute(String ocrResult) {
      mEbtObject.setOcrResult(ocrResult);
      Intent intent = new Intent(mEbtObject, EbtNewNote.class);
      PendingIntent contentIntent = PendingIntent.getActivity(mEbtObject, 0, intent,
              PendingIntent.FLAG_UPDATE_CURRENT);
      NotificationCompat.Builder builder =
              new NotificationCompat.Builder(mEbtObject, "miscellaneous")
                      .setSmallIcon(R.drawable.ic_stat_ebt)
                      .setContentTitle(mEbtObject.getString(R.string.ocr_return))
                      .setContentText(mEbtObject.getString(R.string.ocr_return_detailed))
                      .setAutoCancel(true)
                      .setContentIntent(contentIntent);
      ((NotificationManager) mEbtObject.getSystemService(
              Context.NOTIFICATION_SERVICE)).notify(OCR_NOTIFICATION_ID, builder.build());
   }

   private  String doOcr(Uri uri) {
      String uploadResult = PictureUploader.upload(uri);
//       Log.d(EbtNewNote.LOG_TARGET, "uploadResult: " + uploadResult);

      if (uploadResult.startsWith("Error"))
         return uploadResult;

      String resultUrl = scanPicture(uploadResult);
      Log.d(LOG_TARGET, "resultUrl: " + resultUrl);

      if (resultUrl.startsWith("Error"))
         return resultUrl;

      String ocrResult = "Submitted";
      while (ocrResult.equals("Submitted") || ocrResult.equals("Processing"))
      {
         try
         {
            Thread.sleep(RESULT_POLLING_INTERVAL);
         }
         catch (InterruptedException e)
         {
            Log.e(LOG_TARGET, e.getClass().getSimpleName() + ": " + e.getMessage());
         }

         ocrResult = getOcrResult(resultUrl);
         Log.d(LOG_TARGET, "ocrResult: " + ocrResult);
      }

//      ocrResult = "http://api.ocr-it.com/ocr/v2/download/58dfac97ab4845ad9d1489bdbb703e66.UTF8.TXT";
      if (ocrResult.startsWith("Error"))
         return ocrResult;

      String ocrContent = downloadContent(ocrResult);
      Log.d(LOG_TARGET, "ocrContent: " + ocrContent);

      return TextProcessor.getOcrResult(ocrContent);
   }



   private String
   scanPicture(String url)
   {
      InputStream dis = null;
      try {
         HttpURLConnection connection = (HttpURLConnection) new URL(OCR_HOST + "?wsvKey=" + Keys.OCR_SERVICE).openConnection();
         connection.setReadTimeout(5000);
         connection.setConnectTimeout(5000);
         connection.setRequestMethod("POST");
         connection.setDoInput(true);
         connection.setRequestProperty("Content-Type", "text/xml");
         connection.connect();

         try (OutputStream output = connection.getOutputStream()) {
            output.write(getOcrJobXml(url).getBytes("UTF-8"));
         }
         dis = connection.getInputStream();
      }
      catch (IOException e) {
         Log.e(LOG_TARGET, e.getClass().getSimpleName() + ": " + e.getMessage());
      }
      return extractResultUrl(dis);
   }



   private static String
   getOcrJobXml(String url)
   {
      return
              "<Job>" +
                      "<InputURL>" + url + "</InputURL>" +
                      "<InputType>JPG</InputType>" +
                      "<OCRSettings>" +
                      "<PrintType>OCR_B</PrintType>" +
                      "<LookForBarcodes>false</LookForBarcodes>" +
                      "</OCRSettings>" +
                      "<OutputSettings>" +
                      "<ExportFormat>UnicodeText_UTF8</ExportFormat>" +
                      "</OutputSettings>" +
                      "</Job>";
   }



   private String
   extractResultUrl(InputStream is)
   {
      Document doc = parseXml(is);

      String verificationResult = verifyDocument(doc);
      if (verificationResult.startsWith("Error"))
         return verificationResult;

      String url = getValue("joburl", doc.getFirstChild().getChildNodes());
      if (url == null)
         return "Error: OCR result xml does not contain a 'JobURL' node";

      return url;
   }



   private String
   getOcrResult(String urlString)
   {
      Document doc = null;
      try
      {
         doc = parseXml(new URL(urlString).openStream());
      }
      catch (MalformedURLException e)
      {
         return "Error: " + e.getMessage();
      }
      catch (IOException e)
      {
         return "Error: " + e.getMessage();
      }

      String verificationResult = verifyDocument(doc);
      if (verificationResult.startsWith("Error"))
         return verificationResult;

      NodeList rootChildren = doc.getFirstChild().getChildNodes();

      String status = getValue("status", rootChildren);
      if (status == null)
         return "Error: OCR result XML does not have a 'Status'";

      if (status.equals("Finished"))
         return getOcrDetails(rootChildren);

      if (status.startsWith("Failed"))
      {
         String codeAndMessage = getError(rootChildren);
         Log.e(LOG_TARGET, "OCR failed, status: " + status +
                 ". Code and message: " + codeAndMessage);

         if (status.equals("FailedDownload"))
            return "Error: " + mEbtObject.getString(R.string.ocr_failed_download)   + "\n\n" + codeAndMessage;
         if (status.equals("FailedConversion"))
            return "Error: " + mEbtObject.getString(R.string.ocr_failed_conversion) + "\n\n" + codeAndMessage + "\n\n" + mEbtObject.getString(R.string.ocr_failed_conversion_explanation);
         if (status.equals("FailedNoFunds"))
            return "Error: " + mEbtObject.getString(R.string.ocr_failed_no_funds)   + "\n\n" + codeAndMessage;
         if (status.equals("FailedInternalError"))
            return "Error: " + mEbtObject.getString(R.string.ocr_failed_internal)   + "\n\n" + codeAndMessage;

         return "Error: " + status + ": " + codeAndMessage;
      }
      return status; // "Submitted" or "Processing"
   }



   private static String
   getError(NodeList rootChildren)
   {
      NodeList errorsChildren = getChildren("errors", rootChildren);

      if (errorsChildren == null || errorsChildren.getLength() == 0)
         return ""; // no <Errors> element

      NodeList errorChildren = getChildren("error", errorsChildren);

      return getNodesString(errorChildren);
   }



   private static String
   getOcrDetails(NodeList rootChildren)
   {
      NodeList downloadChildren = getChildren("download", rootChildren);

      if (downloadChildren.getLength() == 0)
         return "Error: OCR result XML does not contain a 'Download'";
//      debugInfo(downloadChildren);

      for (int i = 0; i < downloadChildren.getLength(); ++i)
      {
         if (nodeIs("file", downloadChildren.item(i)))
         {
            NodeList fileChildren = getChildren("file", downloadChildren);
//            debugInfo(fileChildren);
            if (getValue("outputtype", fileChildren).equals("UTF8.TXT"))
               return getValue("uri", fileChildren);
         }
      }

      return "Error: OCR result XML contains 'Download' but not 'File' (with 'OutputType' 'UTF8.TXT')";
   }



   private String
   verifyDocument(Document doc)
   {
      if (doc == null)
         return "Error: DocumentBuildFactory couldn't create Document (see logs)";

      String root = doc.getFirstChild().getNodeName();
//      Log.d(EbtNewNote.LOG_TARGET, "doc.getFirstChild().getNodeName(): " + root);

      if (root.equals("wsvError"))
      {
         String codeAndMessage = getNodesString(doc.getFirstChild().getChildNodes());
         if (codeAndMessage.startsWith("2"))
            return "Error: " + mEbtObject.getString(R.string.ocr_failed_no_funds)
                    + " " + codeAndMessage;
         return "Error: " + codeAndMessage;
      }

      if (root.equals("HTML"))
      {
//         NodeList bodyNodes = getChildren("body", doc.getFirstChild().getChildNodes());
         //NodeList divNodes = getChildren("div", bodyNodes);
      }

      if (root.equals("JobStatus"))
         return "O.K.";

      return "Error: Root node of result XML is not 'JobStatus'";
   }



   /// @see http://www.exampledepot.com/egs/javax.xml.parsers/pkg.html
   private static Document
   parseXml(InputStream is)
   {
      try
      {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         // TODO
//         factory.setValidating(true);
         return factory.newDocumentBuilder().parse(is);
      }
      catch (SAXException e)
      {
         Log.e(LOG_TARGET, "XML is not well-formed: "             + e.getMessage());
      }
      catch (IOException e)
      {
         Log.e(LOG_TARGET, "The parser could not check the XML: " + e.getMessage());
      }
      catch (FactoryConfigurationError e)
      {
         Log.e(LOG_TARGET, "Could not locate a factory class: "   + e.getMessage());
      }
      catch (ParserConfigurationException e)
      {
         Log.e(LOG_TARGET, "Could not locate a JAXP parser: "     + e.getMessage());
      }

      return null;
   }



   private static String
   downloadContent(String url)
   {
      StringBuilder sb = new StringBuilder();
      try
      {
         BufferedReader br = new BufferedReader(new InputStreamReader(
                 new URL(url).openStream()));
         String line = null;
         while ((line = br.readLine()) != null)
            sb.append(line);
      }
      catch (MalformedURLException e)
      {
         return "Error: " + e.getMessage();
      }
      catch (IOException e)
      {
         return "Error: " + e.getMessage();
      }

      return sb.toString();
   }



   private static boolean
   nodeIs(String s, Node n)
   {
      return n.getNodeName().toLowerCase().equals(s);
   }



   private static String
   getValue(String s, NodeList nodeList)
   {
      for (int i = 0; i < nodeList.getLength(); ++i)
         if (nodeIs(s, nodeList.item(i)))
         {
            String value = nodeList.item(i).getFirstChild().getNodeValue();
            Log.d(LOG_TARGET, s + ": " + value);
            return value;
         }

      return null;
   }



   private static List<String>
   getNodeNames(NodeList nodeList)
   {
      List<String> s = new ArrayList<String>();
      for (int i = 0; i < nodeList.getLength(); ++i)
         s.add(nodeList.item(i).getNodeName());

      return s;
   }



   private static String
   getNodesString(NodeList nodeList)
   {
      String s = "";

      for (String nodeName : getNodeNames(nodeList))
      {
         if (! TextUtils.isEmpty(s))
            s += "\n";
         s += nodeName + ": " + getValue(nodeName.toLowerCase(), nodeList);
      }

      return s;
   }



   private static NodeList
   getChildren(String s, NodeList nodeList)
   {
      for (int i = 0; i < nodeList.getLength(); ++i)
         if (nodeIs(s, nodeList.item(i)))
            return nodeList.item(i).getChildNodes();

      return null;
   }



//   private static void
//   debugInfo(NodeList nodeList)
//   {
//      for (int i = 0; i < nodeList.getLength(); ++i)
//      {
//         Log.d(EbtNewNote.LOG_TARGET, "NodeName: " + nodeList.item(i).getNodeName());
////          Log.d(EbtNewNote.LOG_TARGET, "NodeName: " + nodeList.item(i).getNodeValue());
//      }
//   }
}
