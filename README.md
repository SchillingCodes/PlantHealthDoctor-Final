# PlantHealthDoctor-Final
Repository for my work in buidling the Plant Health Doctor application for FAU's Senior Design class.
All work is my own, except where listed in the credits.

This project was awarded the People's Choice Award and the Judge's Choice Award at the Fall 2019 senior design showcase.

Full documentation of the project can be found in the Documentation folder.

I started this project as an amateur in Android programming, machine learning, AWS SageMaker, and bluetooth sensor communication.

## FOLDERS:
#### 1. 3D Housing Design
	DESCRIPTION: Contains the .STL files used to print the 3D housing.

#### 2. Machine Learning Scripts
	DESCRIPTION: Contains the files that were sitting on Amazon SageMaker,
	as well as the data used to build the machine learning model.

	CREDITS:
	mnist_keras_tf.py
		https://www.pyimagesearch.com/2019/01/28/keras-regression-and-cnns/
		https://gitlab.com/juliensimon/dlnotebooks/blob/master/keras/05-keras-blog-post/mnist_keras_tf.py
		ADDED / MODIFIED line #s:
			42
			88-97
			105
			115-116
			127-128
			141-146
			155-167
			198-209
			
#### 3. Android Application
	DESCRIPTION: Code used for the Android phone application.

	CREDITS:
	BluetoothScan.java
		https://github.com/googlearchive/android-BluetoothLeGatt/blob/master/Application/src/main/java/com/example/android/bluetoothlegatt/DeviceControlActivity.java
		ADDED / MODIFIED line #s:
			36-71
			130-262
	BluetoothLeService.java
		https://github.com/googlearchive/android-BluetoothLeGatt/blob/master/Application/src/main/java/com/example/android/bluetoothlegatt/BluetoothLeService.java
		https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23
		ADDED / MODIFIED line #s:
			71-87
			124-134
			175-206
