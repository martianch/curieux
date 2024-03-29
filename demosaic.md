
# Cubic/Bicubic Interpolation for Demosaicing of Bayer-Patterned Color Images

by Marsgazer

## Abstract

The algorithm used in the Curious X3D Viewer for Demosaicing of Bayer-Patterned Color Images
is described. The algorithm uses a combination of cubic and bicubic interpolation.

## Introduction

The _Curious_ X3D viewer is a Java program for viewing images from Mars in X3D stereo
(as RL stereo pairs) on a desktop computer screen. It turned out that the demosaicking
algorithm used in _Curious_ is better than the algorithm used by NASA: it does not
produce "zip" artefacts.

This text documents the algorithm used in the Curious X3D viewer.

This text is documentation from the _Curious X3D Viewer_ ( https://github.com/martianch/curieux/ )
software project and is published among the project's files at github:
https://github.com/martianch/curieux/blob/master/demosaic.md

## Bayer Mosaic

The Bayer mosaic pattern is often described as RGGB, but RG and GB belong to different lines.
There are twice as many green pixels as red or blue.
For our purposes it is important to distinguish green on red lines from green on blue lines,
so we will call them Gr and Gb:  
```
R  Gr R  Gr R  Gr
Gb B  Gb B  Gb B
R  Gr R  Gr R  Gr
Gb B  Gb B  Gb B
R  Gr R  Gr R  Gr
Gb B  Gb B  Gb B
```

If the colors are not decoded, a "Bayered" image looks like a strange gray grid.
If you just make the R pixels red, B pixels blue and Gr/Gb pixels green, you will
get a greenish image (because 50% of the pixels are green), and it will be dark (because
normally each pixel has all three colors rather than just one).
To decode the colors (de-Bayer, demosaic, demosaick), one needs to assign all three colors
to each pixel, that is, to somehow guess each color's brightness looking at the neighboring pixels.


![Fig.1a,1b,1c Bayer grid](https://marsgazer.github.io/curious-help/images/fig1abc.jpg)

<figcaption><b>Fig.1:</b> The Bayer mosaic. <b>1a</b> — colors not decoded; <b>1b</b> — colors decoded, but not interpolated, too much green; <b>1c</b> — a bit less green, but it did not help much: the image is dark, too much dots.
</figcaption>

## Cubic Interpolation

According to https://www.paulinternet.nl/?page=bicubic
```
f(x) = ax^3 + bx^2 + cx + d
f'(x) = 3ax^2 + 2bx + c

f(0) = d
f'(0) = c
f(1) = a + b + c + d
f'(1) = 3a + 2b + c

a = -2f(1) + 2f(0) + f'(1) + f'(0)
b = 3f(1) - 3f(0) - f'(1) - 2f'(0)
c = f'(0)
d = f(0) 
```
These equations let us find f(x) for any x between 0 and 1.
But with Bayer mosaic, we have a very specific particular case.

## Cubic Interpolation for Bayer Mosaic

In a Bayer mosaic line, vertical or horizontal, we have 5 brightness values that represent two different colors:

```q0 p1 q1 p2 q2```

We want to find the brightness value for the color represented by p1 and p2 at the pixel between p1 and p2.

For example, it may be ```G B G B G``` and we want to find the blue value for the central (```G```) pixel.

First of all, we are not interested in _any_ ```x```, we are interested only in ```x=1/2```.

So,
```
x = 1/2

f(1/2) = a/8 + b/4 + c/2 + d =
= (-2f(1) + 2f(0) + f'(1) + f'(0)  +  6f(1) - 6f(0) - 2f'(1) - 4f'(0)  +  4f'(0)  +  8f(0))/8 =
= (4f(1) + 4f(0) - f'(1) + f'(0))/8

f(0) = p1
f(1) = p2
f'(0) = q1 - q0 
f'(1) = q2 - q1 

f(1/2) = (4*p2 + 4*p1 - q2 + 2*q1 - q0)/8 
```

## Where is Cubic Interpolation Enough?

Here's the Bayer mosaic pattern, again:

```
R  Gr R  Gr R  Gr
Gb B  Gb B  Gb B
R  Gr R  Gr R  Gr
Gb B  Gb B  Gb B
R  Gr R  Gr R  Gr
Gb B  Gb B  Gb B
```

There are 3 colors (red, green, blue) and 4 pixel types (```R```, ```Gr```, ```Gb```, ```B```), total 12 cases. 

```
| pixel type | color | interpolation method  |
|------------|-------|-----------------------|
| R          | R     | value already known   |
| R          | G     | cubic in row          |
| R          | B     | bicubic               |
|------------|-------|-----------------------|
|    Gr      | R     | cubic in row          |
|    Gr      | G     | value already known   |
|    Gr      | B     | cubic in column       |
|------------|-------|-----------------------|
| Gb         | R     | cubic in column       |
| Gb         | G     | value already known   |
| Gb         | B     | cubic in row          |
|------------|-------|-----------------------|
|    B       | R     | bicubic               |
|    B       | G     | cubic in row          |
|    B       | B     | value already known   |
|------------|-------|-----------------------|
```

In 6 (of 12) cases, we can use just cubic interpolation, either in a row or in a column.
(This is evident if you look at the Bayer mosaic pattern.)
In 4 cases the color brightness value is already known.
And in 2 cases we need something more elaborate.

## Bicubic interpolation

There are only two cases where cubic interpolation cannot be used:
blue for a red pixel, and red for a blue pixel. 

For example, we want to get the red brightness value for the ```B``` pixel in the center: 

```
      B
Gr R  Gr R  Gr
B  Gb B  Gb B
Gr R  Gr R  Gr
      B
```

We use cubic interpolation in the rows above and below to get the red value 
for the pixels above and below the pixel in the center (the calculated red values
are shown as ```R+```): 

```
      B
Gr R  R+ R  Gr
      B
Gr R  R+ R  Gr
      B
```

Then, we use cubic interpolation in the central column to calculate the red value.

## Implementation

See the class DebayerBicubic (this text is documentation from
the Curious X3D viewer project, https://github.com/martianch/curieux )

## Performance

The cubic interpolation formula executed in the inner loop:

```
( ((p1() + p2()) << 2) + (q1() << 1) - q0() - q2() ) >> 3
```

contains only addition/subtraction and shifts.

Java uses 8-bit color intensity values and 32-bit integers, so overflow is not a problem here.
(With 32-bit color intensity values, it would be a problem, and one would have to e.g. use 64-bit math.)

With modern hardware and software, the performance mostly depends on how well the CPU cache
and the JIT compiler (just-in-time Java bytecode to native code compiler) do their job. 

(Probably one should elaborate on the above for readers with no Java background. Historically, 
multiplication and division used to be much slower than addition, subtraction and shifts.
It is possible that some embedded systems still use hardware with slow multiplication 
and division, but on modern CPUs division and multiplication are not slower than other operations.
The expression is simple to calculate, but this is not important on the modern CPUs.
Well, if you are making custom hardware, this _may_ be of importance.
What is important on modern CPUs is memory access time. Reading memory is much slower than
calculating expressions, and this problem is addresses by L1 and L2 caches.
Even if the expression was more complex, the overall performance would still be determined
by the performance of L1 and L2 caches. 
You also could mention that the values in the formula are accessed like ```p1()``` rather than ```p1```,
these are method calls rather than variable reads. It is the JIT compiler's job to optimize this.
In theory, the JIT compiler is able to inline methods calls (but in practice you never know
whether or not a theoretically possible optimization applies to your particular case).
The bottom line is: if you _are_ making custom hardware, it is easy to get a good performance.
If you use mainstream hardware, this demosaicing is approximately as fast as any other
image transformation.)

## Discussion

We use this algorithm to demosaick images sent from Mars. It is important
to be able to zoom the image and see small details: the rovers on Mars usually will not
change the course to make a better shot of whatever we find interesting even if it would be
technically possible. 

So one of the design goals was to minimize the spot within which the neighboring pixels
affect the estimated color intensity.

This algorithm produces less demosaicking artefacts than the Malvar [1] algorithm currently used [2] by NASA.

![Fig.2,3,4](https://marsgazer.github.io/curious-help/images/fig2ab3ab4.jpg)

<figcaption><b>Fig.2</b> — an image decoded by NASA, <b>2a</b> — without color correction, <b>2b</b> — after applying color correction; <b>Fig.3</b> — the same image decoded by x3dview, with the algorighm proposed in this paper, <b>3a</b> — without color correction, <b>3b</b> — after applying color correction; <b>Fig.4</b> — the same object photographed at a different time with a different camera with a better resolution.</figcaption>

## References

[1] H.S. Malvar, L. He, R. Cutler, “High-quality linear interpolation for demosaicing of Bayer-patterned
color images”, in Proceedings of IEEE International Conference on Acoustics, Speech and
Signal Processing (ICASSP), 2004.
https://www.researchgate.net/publication/4087683_High-quality_linear_interpolation_for_demosaicing_of_Bayer-patterned_color_images

[2] https://agupubs.onlinelibrary.wiley.com/doi/full/10.1002/2016EA000219
