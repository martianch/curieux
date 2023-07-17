
# Curious: an X3D Viewer

Designed to view the Curiosity Rover images from Mars in X3D, but can be used to view any stereo pairs (both LR and X3D).
Opens images from Internet or local drive, supports drag-and-drop, for example, you can drag-n-drop the red DOWNLOAD
button from the raw images index on the NASA site.

[Download this software.](https://github.com/martianch/curieux/releases)

Direct link to Curiosity raw images: https://mars.nasa.gov/msl/multimedia/raw-images/?order=sol+desc%2C+date_taken+desc%2Cinstrument_sort+asc%2Csample_type_sort+asc&per_page=100&page=0&mission=msl

**UPDATE** [A site with documentation (or rather tutorial)](https://marsgazer.github.io/curious-help/) for this program,
showing how to use it.

## What is X3D Stereo and How To View It

Blog post (explains how to make a viewing frame from an A4 sheet): https://corgegrault.blogspot.com/2020/02/what-is-x3d-and-how-to-view-it-just-on.html

Video: https://www.youtube.com/watch?v=hxTMlDijDlU
"Learn how to view upcoming images in 3D without using ANY special GLASSES! I would advise all to do this on nothing smaller than a laptop screen.  Especially when learning.
Bigger is usually Better." 

"The finger method" of learning X3D viewing: https://www.whatsupinthesky.com/images/dgannett/3D_TUTORIALS/X3D.jpg

(In case of discomfort: the distance is wrong, find a comfortable distance)

The simplest explanation: an X3D stereo pair is a R L stereo pair (the right image is for the left eye).
To view it, conceal the right image from the right eye with the right hand, and conceal the left image from the left eye with the left hand.
[Like this.](https://1.bp.blogspot.com/-PhMNZxEyuVE/XlAqaTOKAeI/AAAAAAAAAQ0/UMxy463wwpUsSHS40IJuJtweWaSwWGhsgCLcBGAsYHQ/s1600/stereo-tits.png)

To make a stereo pair: make a photo, step right, make a photo.

## Running This Software

### The Easiest Way: Running Without Installation

1. Install Java. You will need **Java 8+** to run it. (For example, download OpenJDK from https://openjdk.java.net/projects/jdk/17/ ) Run `java -version` in the command line to check it. 
2. Download x3dview.zip and unpack it to some directory. You must get the file `x3dview.jar` (well, it will be in a subdirectory, `x3dview/lib/`).
3. From the command line, run `java -jar x3dview.jar`. To do so, you must be in the directory where x3dview.jar is located.

Why command line? The commercial operating systems do not trust free software. In fact, they just hate it. "Gosh, you downloaded something from the Internet, you (panic in the voice) want to run it? Oh please, please, don't! It's not signed!" And to sign it, the author of free software must pay them. I find it immoral. It is immoral to require the author of free software to pay. (And there are reasons to call paid distribution of such software immoral. You see, something very bad happened on Mars, and it is immoral to make money on that event.) So you have to do a little bit of system administration yourself.

### Running from the source

If your environment is so configured that you cannot download Java software, you still can run the software
from the source code, but the buttons will have text instead of icons. (You might think that this sounds crazy,
but computer labs sometimes have system administrators that fight with viruses and students by restricting
the students' access rights. Note that if it is your employer that restricted your access rights, 
your internet traffic is likely also monitored.)

Anyway, starting from Java 11, you can run any single-file program just from the source,
providing the path if necessary:

```
java Main.java

java src/main/java/com/github/martianch/curieux/Main.java

/usr/lib/jvm/java-13-oracle/bin/java src/main/java/com/github/martianch/curieux/Main.java
```

The file Main.java can be downloaded from the source tree of this project: [direct link](https://raw.githubusercontent.com/martianch/curieux/master/src/main/java/com/github/martianch/curieux/Main.java).
In the worst case you will have to copy and paste the whole source into the text editor.
(Yes, this software is a single-file program. Running from source is a feature that I do not want to drop.)


### Installation

Unpack the file x3dview.zip to some directory. You will see a new directory, `x3dview`, with two subdirectories: `bin` and `lib`.
If you use Linux or Darwin, 
```
chmod a+x x3dview/bin/x3dview
```

You will need **Java 8+** to run it. Run `java -version` to check it. 

(For experienced users: If you use Linux and have Java 11+, you may run the file Main.java as a shebang script (if you know what is shebang and don't mind text instead of icons). Or you may use `java -jar` if you prefer this method. Or you may run the script. Any of these methods should work, no difference. Then why several methods of running? There are very exotic system configurations, like "the default java must be v6 in our bank", and it is easier to provide a workaround if there are multiple ways to run the program.)

### (Optional) Registering X3DView as a Graphics Application
#### Ubuntu Linux
Create the file `~/.local/share/applications/x3dview.desktop` with the following contents:
```
[Desktop Entry]
Version=0.1.2
Type=Application
Name=X3D Viewer
#Icon=/full/path/xxx.svg
# !!! specify the correct path below !!!
Exec="/home/me/x/x3dview/bin/x3dview" %F
Comment=View Images as Stereo Pairs
Categories=Graphics;2DGraphics;3DGraphics;RasterGraphics;Viewer;
Terminal=false
```

After that, you will be able to select one or two image files, click the right mouse button on the selection,
select "Open With Other Application" from the menu, select "X3D Viewer", and have the application run.

#### Windows, Mac
TBD


### Running With Command-Line Arguments

You need to read this section only if you want to invoke this software from a script.

#### Two Arguments: Specify Any Two Files

`x3dview file1 file2` - show file1 and file2 as a stereo pair

`x3dview path1/file1 path2/file2` - show file1 and file2 as a stereo pair, using absolute or relative path

`x3dview url1 url2` - show files at url1 and url2 as a stereo pair

`java -jar ~/path/to/x3dview.jar file1 file2` - you can run it using `java -jar`, there is no difference

Linux example:
`java -jar ~/x/x3dview/lib/x3dview.jar NLB_633993124EDR_F0790000NCAM00289M_.JPG NRB_633993124EDR_F0790000NCAM00289M_.JPG`

A Windows example:

`\soft\x3dview\bin\x3dview https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02693/opgs/edr/fcam/FLB_636566396EDR_F0790294FHAZ00302M_.JPG https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02693/opgs/edr/fcam/FRB_636566396EDR_F0790294FHAZ00302M_.JPG` - a Windows example where the full path of the executable is specified with two image URLs. Note that it is `x3dview.bat` that executes under Windows!

#### One Argument: Guess the Match

For Curiosity images, in many cases the second character in the image file name is either R or L, and the corresponding file exists in the same directory.

`x3dview file1` - show file1 with its pair (guess file2 from file1)

`x3dview path1/file1` - guess file2 from file1, show path1/file1 and path1/file2 as a stereo pair, using absolute or relative path

`x3dview url1` - show the file at url1 with its pair (guess url2 from url1)

A Windows example:

`\soft\x3dview\bin\x3dview https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02693/opgs/edr/fcam/FLB_636566396EDR_F0790294FHAZ00302M_.JPG` - guess the second URL, the only difference is R or L (the same two images as in the previous section).

#### No Arguments: Drag and Drop

When you start it as `x3dview`, two default images will be downloaded and shown.

You can **Drag-and-Drop** any **two files** onto the view, they will be shown as a stereo pair.

If you **Drag-and-Drop** only **one file**, the result will depend on the **"DnD to Both" checkbox**. If it is checked, the pair will be guessed from the file name. If it is not checked, the image will be shown in the pane where it was dropped.

In the same way, you can Drag-and-Drop an URL by dragging either the address from the browser address bar or the image itself from the view, or a link (for example, you can Drag-and-Drop the red "DOWNLOAD" button on the NASA site).

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/martianch/curieux/tags).
In general, the "semantic versioning" is evil because it makes software developers think that incompatibilities are permissible if you increment the leftmost digit,
but in our case there is no API, only UI. And the plan is to never increment the leftmost zero.

## Building

To build it as is, you need Java 13. At the moment, Gradle does not work with Java 14.
Alternatively, you can compile it without Gradle, there is only one file: Main.java (or you may just run it as a script).
But you need Gradle to run the unit tests. And you need gradle to create a .jar and to pass the version tag to the application
(the version is shown in the bottom of the help screen).

## License

This software is Public Domain.

This project on GitHub: https://github.com/martianch/curieux



