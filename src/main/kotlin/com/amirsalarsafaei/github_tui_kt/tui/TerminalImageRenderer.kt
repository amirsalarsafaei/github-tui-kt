package com.amirsalarsafaei.github_tui_kt.tui

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.sksamuel.scrimage.ImmutableImage
import java.io.File

class TerminalImageRenderer {
    companion object {
        fun imageToString(path: File, width: Int = 80): String {
            val image = ImmutableImage.loader().fromFile(path)
            return _imageToString(image, width)
        }

        fun splashImageString( width: Int = 80): String {
            val image = ImmutableImage.loader().fromResource("/splash.png")
            return _imageToString(image, width)
        }

        internal fun _imageToString(image: ImmutableImage, width: Int): String{
            val aspectRatio = image.height.toDouble() / image.width
            val height = (width * aspectRatio * 0.5).toInt() // Adjust for character aspect ratio
            val scaled = image.scaleTo(width, height)

            return buildString {
                for (y in 0 until scaled.height) {
                    for (x in 0 until scaled.width) {
                        val pixel = scaled.pixel(x, y)

                        val char = when {
                            pixel.alpha() == 0 -> " "
                            pixel.alpha()  <= 0.25 -> "░"
                            pixel.alpha()  <= 0.5 -> "▒"
                            pixel.alpha() <= 0.75 -> "▓"
                            else -> "█"
                        }

                        val r = pixel.red()
                        val g = pixel.green()
                        val b = pixel.blue()


                        append(TextColors.rgb(r / 255.0f, g / 255.0f, b / 255.0f)(char))
                    }
                    append("\n")
                }
            }
        }
    }
}