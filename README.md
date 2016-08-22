# DepthSensor-API-on-Android
A Tutorial and Code snippet on how to get your Depth Sensor running on Android all the way up to 3rd party apps

Depth Sensor Camera is at the core of many 3D/AR/VR technologies. A Depth Sensor camera allows scanning the real 3D world/objects in front of the camera and creating a "Depth" map, which can then be translated into a 3D scan. A Simple intuitive application of this is a "Night Vision Goggles".
See  https://3dprint.com/117809/depth-sensing-phone-cameras/ for why Depth Sensor Cameras are the going to be the rage.


The software enabler framework for integrating a Depth Sensor camera is shown below. 
![alt tag](https://github.com/mahamannu/DepthSensor-API-on-Android/blob/master/DSC.png)
In your modified Android OS/Framework implementation, you need to include the DSC services which talk directly with a wrapper around DepthSensor SDK such as SoftKinetic ( https://www.softkinetic.com/ ). These SDKs are integrated on the Linux side and the framework/SDK/API to expose their functionality to Apps running on your Modified Android Hardware is usually missing. 
This tutorial/Code samples aims to solve that missing piece.


At the core of the implementation is an Android Shared memory implementation , such as the one described here @ 
http://www.androidenea.com/2010/03/share-memory-using-ashmem-and-binder-in.html
This is used to transfer DepthSensor Data ( 3D scan Data ) from Linux world to Android Framework ( and then to Apps ) and we have to do so without copying/passing the data around. Shared Memory is the only performance-sensitive solution. 


