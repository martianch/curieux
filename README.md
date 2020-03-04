# curieux

This software is Public Domain.

## Curious: an X3D Viewer
Designed to view the Curiosity Rover images from Mars in X3D, but can be used to view any stereo pairs (both LR and X3D).
Opens images from Internet or local drive, supports drag-and-drop, for example, you can drag-n-drop the red DOWNLOAD
button from the raw images index on the NASA site.

###What is X3D Stereo and How To View It

Blog post (explains how to make a viewing frame from an A4 sheet):

Video:

Video:

The simplest explanation: in an X3D stereo pair is a R L stereo pair (the right image is for the left eye).
To view it, conceal the right image from the right eye with the right hand, and conceal the left image from the left eye with the left hand.


###Installation

Unpack the file x3dview.zip to some directory. You will see a new directory, `x3dview`, with two subdirectories: `bin` and `lib`.
If you use Linux or Darwin, 
```
chmod a+x x3dview/bin/x3dview
```

You will need Java 8+ to run it. If you use Linux and have Java 11+, you may run the file Main.java as a shebang script (if you know what is shebang).

###Running

####Two Arguments: Specify Any Two Files

`x3dview file1 file2` - show file1 and file2 as a stereo pair

`x3dview path1/file1 path2/file2` - show file1 and file2 as a stereo pair, using absolute or relative path

`x3dview url1 url2` - show files at url1 and url2 as a stereo pair

`\soft\x3dview\bin\x3dview https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02693/opgs/edr/fcam/FLB_636566396EDR_F0790294FHAZ00302M_.JPG https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02693/opgs/edr/fcam/FRB_636566396EDR_F0790294FHAZ00302M_.JPG` - a Windows example where the full path of the executable is specified with two image URLs. Note that it is `x3dview.bat` that executes under Windows!

####One Argument: Guess the Match

For Curiosity images, in many cases the second character in the image file name is either R or L, and the corresponding file exists in the same directory.

`x3dview file1` - show file1 with its pair (guess file2 from file1)

`x3dview path1/file1` - guess file2 from file1, show path1/file1 and path1/file2 as a stereo pair, using absolute or relative path

`x3dview url1` - show the file at url1 with its pair (guess url2 from url1)

`\soft\x3dview\bin\x3dview https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02693/opgs/edr/fcam/FLB_636566396EDR_F0790294FHAZ00302M_.JPG` - guess the second URL, the only difference is R or L (the same two images as in the previous section).

####No Arguments: Drag and Drop

When you start it as `x3dview`, two default images will be downloaded and shown.

You can Drag-and-Drop any two files onto the view, they will be shown as a stereo pair.

If you Drag-and-Drop only one file, the result will depend on the "DnD to Both" checkbox. If it is checked, the pair will be guessed from the file name. If it is not checked, the image will be shown in the pane where it was dropped.

In the same way, you can Drag-and-Drop an URL by dragging either the address from the browser address bar or the image itself from the view, or a link (for example, you can Drag-and-Drop the red "DOWNLOAD" button on the NASA site).

### Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/martianch/curieux/tags).
In general, the "semantic versioning" is evil because it makes software developers think that incompatibilities are permissible if you increment the leftmost digit,
but in our case there is no API, only UI.









