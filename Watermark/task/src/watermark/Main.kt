package watermark

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import javax.imageio.ImageIO


fun main()  {
    println("Input the image filename:")
    val fileName = readln()
    val file = File(fileName)
    val imageFile: BufferedImage
    try{
        imageFile = ImageIO.read(file)
        if(!prov(imageFile)) return
    } catch(e: Exception) {
        println("The file $fileName doesn't exist.")
        return
    }
    println("Input the watermark image filename:")
    val fileNameWatermark = readln()
    val fileWatermark = File(fileNameWatermark)
    val watermarkFile: BufferedImage
    try {
        watermarkFile = ImageIO.read(fileWatermark)
        if(!provWatermark(watermarkFile, imageFile)) return
    } catch(e: Exception) {
        println("The file $fileNameWatermark doesn't exist.")
        return
    }

    var alpha: Int = 0
    var colorsTransparent = "0 0 0"
    if(watermarkFile.transparency == BufferedImage.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        val alphaString = readln().lowercase()
        if(alphaString == "yes") alpha = 2
    } else {
        println("Do you want to set a transparency color?")
        val colors = readln().lowercase()
        if(colors == "yes") {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            val str = readln()
            val str2 = str.split(" ")
            if (str2.size != 3) {
                println("the transparency color input is invalid.")
                return
            }
            str2.forEach { if (it.toInt() in 0..255) else {
                println("The transparency color input is invalid.")
                return
                }
            }
            colorsTransparent = str
            alpha = 1
        } else {
            alpha = 0
        }
    }

    println("Input the watermark transparency percentage (Integer 0-100):")
    val percentage: Int
    try {
        percentage = readln().toInt()
        if(percentage !in 0..100) {
            println("The transparency percentage is out of range.")
            return
        }
    } catch(e: Exception) {
        println("The transparency percentage isn't an integer number.")
        return
    }

    println("Choose the position method (single, grid):")
    val position = readln()
    if(position != "single" && position != "grid") {
        println("The position method input is invalid.")
        return
    }
    val diff: String
    if(position == "single") {
        val diffX = imageFile.width - watermarkFile.width
        val diffY = imageFile.height - watermarkFile.height
        println("Input the watermark position ([x 0-$diffX] [y 0-$diffY]):")
        diff = readln()
        val diff2: List<Int>
        try{
            diff2 = diff.split(" ").map { it.toInt() }
        } catch (e: Exception) {
            println("The position input is invalid.")
            return
        }
        if (diff2.size != 2) {
            println("The position input is invalid.")
            return
        }
        if(diff2[0] !in 0..diffX || diff2[1] !in 0..diffY) {
            println("The position input is out of range.")
            return
        }
    } else {
        diff = ""
    }

    println("Input the output image filename (jpg or png extension):")
    val rezFile = readln()
    val regex = ".*\\.[pPjJ][pPnN][gG]".toRegex()
    if(!rezFile.matches(regex)) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        return
    }

    create(imageFile, watermarkFile, percentage, rezFile, alpha, colorsTransparent, position, diff)
}

fun create(imageFile: BufferedImage, imageFileWatermark: BufferedImage, percentage: Int, rezFile: String, alpha: Int,
           colorsTransparent: String, position: String, diff: String) {
    val prom = colorsTransparent.split(" ")
    val transparent = Color(prom[0].toInt(), prom[1].toInt(), prom[2].toInt())

    val rez: BufferedImage = if(position == "single") {
        createImageSingle(imageFile, imageFileWatermark, percentage, alpha, transparent, diff)
    } else {
        createImageGrid(imageFile, imageFileWatermark, percentage, alpha, transparent)
    }
    inputImage(rez, rezFile)
}

fun createImageSingle(imageFile: BufferedImage, imageFileWatermark: BufferedImage, percentage: Int, alpha: Int,
                      transparent: Color, diff: String): BufferedImage{
    val rez: BufferedImage = imageFile
    val (diffX, diffY) = diff.split(" ").map { it.toInt() }
    for(x in diffX until diffX + imageFileWatermark.width) {
        for (y in diffY until diffY + imageFileWatermark.height) {
            val i = Color(imageFile.getRGB(x, y))
            val w = Color(imageFileWatermark.getRGB(x - diffX, y - diffY))
            val wT = Color(imageFileWatermark.getRGB(x - diffX, y - diffY), true)
            when(alpha) {
                0 -> rez.setRGB(x, y, normalColor(i, w, percentage).rgb)
                1 -> rez.setRGB(x, y,
                        setAlphaColor(i, w, percentage, transparent).rgb)
                2 -> rez.setRGB(x, y, alphaColor(i, wT, percentage).rgb)
            }
        }
    }
    return rez
}

fun createImageGrid(imageFile: BufferedImage, imageFileWatermark: BufferedImage, percentage: Int, alpha: Int,
                    transparent: Color): BufferedImage {
    val rez: BufferedImage = BufferedImage(imageFile.width, imageFile.height, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until imageFile.width) {
        for (y in 0 until imageFile.height) {

            var xWater = x
            var yWater = y
            while (true) {
                if(xWater >= imageFileWatermark.width) {
                    xWater -= imageFileWatermark.width
                } else break
            }
            while (true) {
                if(yWater >= imageFileWatermark.height) {
                    yWater -= imageFileWatermark.height
                } else break
            }
            val i = Color(imageFile.getRGB(x, y))
            val w = Color(imageFileWatermark.getRGB(xWater, yWater))
            val wT = Color(imageFileWatermark.getRGB(xWater, yWater), true)
            when(alpha) {
                0 -> rez.setRGB(x, y, gridNormal(i, w, imageFile, imageFileWatermark, percentage).rgb)
                1 -> rez.setRGB(x, y,
                        gridSetAlpha(i, w, imageFile, imageFileWatermark, percentage, transparent).rgb)
                2 -> rez.setRGB(x, y, gridAlpha(i, wT, imageFile, imageFileWatermark, percentage).rgb)
            }
        }
    }
    return rez
}

fun gridNormal(i: Color, w: Color, imageFile: BufferedImage, imageFileWatermark: BufferedImage, percentage: Int): Color {
    return Color(
        (percentage * w.red + (100 - percentage) * i.red) / 100,
        (percentage * w.green + (100 - percentage) * i.green) / 100,
        (percentage * w.blue + (100 - percentage) * i.blue) / 100
    )
}

fun gridAlpha(i: Color, w: Color, imageFile: BufferedImage, imageFileWatermark: BufferedImage, percentage: Int): Color {
    return if(w.alpha == 255) {
        val color = Color (
            (percentage * w.red + (100 - percentage) * i.red) / 100,
            (percentage * w.green + (100 - percentage) * i.green) / 100,
            (percentage * w.blue + (100 - percentage) * i.blue) / 100
        )
        color
    } else {
        i
    }
}

fun gridSetAlpha(i: Color, w: Color, imageFile: BufferedImage, imageFileWatermark: BufferedImage,
         percentage: Int, transparent: Color): Color{
    return if(w == transparent) {
        i
    } else {
        val color = Color (
            (percentage * w.red + (100 - percentage) * i.red) / 100,
            (percentage * w.green + (100 - percentage) * i.green) / 100,
            (percentage * w.blue + (100 - percentage) * i.blue) / 100
        )
        color
    }
}

fun setAlphaColor(i: Color, w: Color, percentage: Int, transparent: Color): Color {
    return if(w == transparent) {
        i
    } else {
        val color = Color (
            (percentage * w.red + (100 - percentage) * i.red) / 100,
            (percentage * w.green + (100 - percentage) * i.green) / 100,
            (percentage * w.blue + (100 - percentage) * i.blue) / 100
        )
        color
    }
}

fun alphaColor(i: Color, w: Color, percentage: Int): Color {

    return if(w.alpha == 255) {
        val color = Color (
            (percentage * w.red + (100 - percentage) * i.red) / 100,
            (percentage * w.green + (100 - percentage) * i.green) / 100,
            (percentage * w.blue + (100 - percentage) * i.blue) / 100
        )
        color
    } else {
        i
    }
}

fun normalColor(i: Color, w: Color, percentage: Int): Color {
    return Color(
        (percentage * w.red + (100 - percentage) * i.red) / 100,
        (percentage * w.green + (100 - percentage) * i.green) / 100,
        (percentage * w.blue + (100 - percentage) * i.blue) / 100
    )

}

fun inputImage(rezImage: BufferedImage, rezFile: String) {
    println("The watermarked image $rezFile has been created.")
    val fileRez = File(rezFile)
    ImageIO.write(rezImage, "png", fileRez)
}



fun provWatermark(imageFileWatermark: BufferedImage, imageFile: BufferedImage): Boolean {
    return if(imageFileWatermark.colorModel.numColorComponents != 3) {
        println("The number of watermark color components isn't 3.")
        false
    } else if (imageFileWatermark.colorModel.pixelSize != 24 && imageFileWatermark.colorModel.pixelSize != 32) {
        println("The watermark isn't 24 or 32-bit.")
        false
    } else if (imageFileWatermark.width > imageFile.width || imageFileWatermark.height > imageFile.height) {
        println("The watermark's dimensions are larger.")
        false
    } else true
}
fun prov(imageFile: BufferedImage): Boolean {
    return if(imageFile.colorModel.numColorComponents != 3) {
        println("The number of image color components isn't 3.")
        false
    } else if (imageFile.colorModel.pixelSize != 24 && imageFile.colorModel.pixelSize != 32) {
        println("The image isn't 24 or 32-bit.")
        false
    } else true
}

/*
Input the image filename:
> test\image5.png
Input the watermark image filename:
> test\logorgb.png
Do you want to set a transparency color?
> yes
Input a transparency color ([Red] [Green] [Blue]):
> 0 0 0
Input the watermark transparency percentage (Integer 0-100):
> 35
Choose the position method (single, grid):
> single
Input the watermark position ([x 0-300] [y 0-600]):
> 200 200
Input the output image filename (jpg or png extension):
> test\out3.png
The watermarked image test\out3.png has been created.
 */
