package com.example.planthealthdoctor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.ArrayMap;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.services.sagemakerruntime.*;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sagemakerruntime.model.InvokeEndpointRequest;
import com.amazonaws.services.sagemakerruntime.model.InvokeEndpointResult;
import com.amazonaws.services.sagemakerruntime.model.ModelErrorException;

import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class MainActivity extends AppCompatActivity {

    private BasicAWSCredentials credentials = new BasicAWSCredentials("AKIA4BY2MYUBOQBDZXNW", "FMBF5sL+AnlDwCdAOczk8RcZnj7rpKZiN+bpnBFM");

    static {
        OpenCVLoader.initDebug();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView iv = findViewById(R.id.oval1);
        ImageView iv2 = findViewById(R.id.oval2);

        RotateAnimation ra = new RotateAnimation(0.0f,360.0f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(10000);
        ra.setInterpolator(new LinearInterpolator());
        ra.setRepeatCount(Animation.INFINITE);
        ra.setRepeatMode(Animation.RESTART);

        RotateAnimation ra2 = new RotateAnimation(360.0f,0.0f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra2.setDuration(8000);
        ra2.setInterpolator(new LinearInterpolator());
        ra2.setRepeatCount(Animation.INFINITE);
        ra2.setRepeatMode(Animation.RESTART);

        iv.startAnimation(ra);
        iv2.startAnimation(ra2);
    }

    public void doSomething() {


        class RetrieveFeedTask extends AsyncTask<InvokeEndpointRequest, Void, InvokeEndpointResult> {

            private Exception exception;

            protected InvokeEndpointResult doInBackground(InvokeEndpointRequest... req) {
                try {
                    AmazonSageMakerRuntimeClient client = new AmazonSageMakerRuntimeClient(credentials);
                    System.out.println(req[0]);
                    return client.invokeEndpoint(req[0]);
                } catch (ModelErrorException e) {
                    this.exception = e;
                    System.out.println(e.getOriginalMessage());
                    return null;
                }
            }
        }

        File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFileName);
        System.out.println(f.toString());
        if (!f.exists()) try {

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Mat image = imread(f.getPath(), IMREAD_COLOR);
        Size sz = new Size(64,64);
        Mat resizeImage = new Mat();
        Imgproc.resize( image, resizeImage, sz);

        double temp[][][][] = new double[1][resizeImage.width()][resizeImage.height()][resizeImage.depth()];
        for (int i = 0; i < resizeImage.width(); i++) {
            for (int j = 0; j < resizeImage.height(); j++) {
                temp[0][i][j] = resizeImage.get(i,j);
            }
        }

        Map<String, double[][][][]> m = new ArrayMap<>();
        m.put("instances", temp);

        try {
            JSONObject ja = new JSONObject(m);
            byte[] b = ja.toString().getBytes("UTF-8");
            System.out.println(b.length);
            ByteBuffer bb = ByteBuffer.allocate(b.length);
            bb.put(b);
            bb.position(0);

            InvokeEndpointRequest req = new InvokeEndpointRequest().withAccept("application/json").withContentType("application/json").withEndpointName("awesome-plant-health-endpoint").withBody(bb);
            InvokeEndpointResult ier = new RetrieveFeedTask().execute(req).get();
            String s = new String(ier.getBody().array(), StandardCharsets.UTF_8 );

            Intent intent = new Intent(this, BluetoothScan.class);
            intent.putExtra("pHPrediction", s);
            startActivity(intent);

        } catch(Exception e) {
            Intent intent = new Intent(this, BluetoothScan.class);
            intent.putExtra("pHPrediction", "bad");
            startActivity(intent);
        }
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    static final int REQUEST_TAKE_PHOTO = 1;

    public void  doSomething2(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.planthealthdoctor.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    String currentPhotoPath;
    String imageFileName;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "test.jpg";
        File image = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFileName);

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            doSomething();
        }
    }

    public void logout(View view) {
        AWSMobileClient.getInstance().signOut();
        Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
        startActivity(intent);
    }


}
