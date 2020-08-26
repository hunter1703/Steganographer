package temp;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.awt.image.BufferedImage.TYPE_INT_BGR;

/**
 * User : rahul
 * Date : 18/06/20
 * Time : 7:44 PM
 */
public class ImageSteganographer {

    private static final int IMAGE_TYPE = TYPE_INT_BGR;

    public static void encode(final String keyPath, final String imagePath, final String targetPath) throws IOException {
        if (!targetPath.endsWith(".png")) {
            throw new IllegalArgumentException("target file should be a png file");
        }
        final String[] split = imagePath.split("/");
        final String name = split[split.length - 1];
        final BufferedImage initialKey = ImageIO.read(new File(keyPath));
        //all image dimensions should be 2 bytes long only (65536 x 65536)
        final BufferedImage image = ImageIO.read(new File(imagePath));
        int keyWidth = initialKey.getWidth();
        final int imageWidth = image.getWidth();
        int keyHeight = initialKey.getHeight();
        int imageHeight = image.getHeight();
        if ((imageWidth >> 16) > 0 || (imageHeight >> 16) > 0) {
            throw new IllegalStateException("Image to be hidden have dimensions greater than 2 bytes");
        }
        if ((name.length() >> 8) > 0) {
            throw new IllegalArgumentException("Image to be hidden have name greater than 1 byte");
        }
        final double heightMultiplier = Math.ceil((1.0 * imageHeight) / keyHeight);
        final double widthMultiplier = Math.ceil((1.0 * imageWidth) / keyWidth);
        final int multiplier = (int) Math.max(heightMultiplier, widthMultiplier);
        final BufferedImage key = enlargeKeyImageIfNeeded(initialKey, multiplier);
        keyWidth *= multiplier;
        keyHeight *= multiplier;
        final BufferedImage target = new BufferedImage(2 * keyWidth, 2 * keyHeight, IMAGE_TYPE);


        for (int i = 0; i < keyWidth; i++) {
            for (int j = 0; j < keyHeight; j++) {
                final int xx = 2 * i;
                final int yy = 2 * j;
                final int keyRGB = key.getRGB(i, j) & 0x00ffffff;
                final int rM = (keyRGB >> 16) & 0b11111100;
                final int gM = (keyRGB >> 8) & 0b11111100;
                final int bM = keyRGB & 0b11111100;

                final int imageRGB = i < imageWidth && j < imageHeight ? image.getRGB(i, j) & 0x00ffffff : 0;

                final int imageR = (imageRGB >> 16) & 0xff;
                final int imageRMM = (imageR & 0b11000000) >> 6;
                final int imageRML = (imageR & 0b00110000) >> 4;
                final int imageRLM = (imageR & 0b00001100) >> 2;
                final int imageRLL = imageR & 0b00000011;

                final int imageG = (imageRGB >> 8) & 0xff;
                final int imageGMM = (imageG & 0b11000000) >> 6;
                final int imageGML = (imageG & 0b00110000) >> 4;
                final int imageGLM = (imageG & 0b00001100) >> 2;
                final int imageGLL = imageG & 0b00000011;

                final int imageB = imageRGB & 0xff;
                final int imageBMM = (imageB & 0b11000000) >> 6;
                final int imageBML = (imageB & 0b00110000) >> 4;
                final int imageBLM = (imageB & 0b00001100) >> 2;
                final int imageBLL = imageB & 0b00000011;

                for (int x = xx; x < xx + 2; x++) {
                    for (int y = yy; y < yy + 2; y++) {
                        int r, g, b;
                        if (x % 2 == 0 && y % 2 == 0) {
                            r = rM | imageRMM;
                            g = gM | imageGMM;
                            b = bM | imageBMM;
                        } else if (x % 2 == 0 && y % 2 == 1) {
                            r = rM | imageRML;
                            g = gM | imageGML;
                            b = bM | imageBML;
                        } else if (x % 2 == 1 && y % 2 == 0) {
                            r = rM | imageRLM;
                            g = gM | imageGLM;
                            b = bM | imageBLM;
                        } else {
                            r = rM | imageRLL;
                            g = gM | imageGLL;
                            b = bM | imageBLL;
                        }
                        target.setRGB(x, y, r << 16 | g << 8 | b);
                    }
                }
            }
        }

        //encode image resolution
        encodeImageResolution(imageWidth, imageHeight, target);
        //name should only be 1 byte long (256 chars)
        encodeName(name, target);
        ImageIO.write(target, "png", new File(targetPath));
    }

    public static void decode(final String sourcePath, final String targetDirectory) throws IOException {
        final BufferedImage source = ImageIO.read(new File(sourcePath));
        ImageIO.createImageOutputStream(new File(sourcePath)).length();
        final Tuple dimensions = decodeImageResolution(source);
        final BufferedImage target = new BufferedImage(dimensions.width, dimensions.height, IMAGE_TYPE);
        for (int i = 0; i < source.getWidth(); i += 2) {
            for (int j = 0; j < source.getHeight(); j += 2) {
                int r, g, b;
                int rMM = 0, rML = 0, rLM = 0, rLL = 0;
                int gMM = 0, gML = 0, gLM = 0, gLL = 0;
                int bMM = 0, bML = 0, bLM = 0, bLL = 0;
                for (int x = i; x < i + 2; x++) {
                    for (int y = j; y < j + 2; y++) {
                        final int sourceRGB = source.getRGB(x, y);
                        r = (sourceRGB >> 16) & 0xff;
                        g = (sourceRGB >> 8) & 0xff;
                        b = sourceRGB & 0xff;
                        if (x % 2 == 0 && y % 2 == 0) {
                            rMM = r & 0x3;
                            gMM = g & 0x3;
                            bMM = b & 0x3;
                        } else if (x % 2 == 0 && y % 2 == 1) {
                            rML = r & 0x3;
                            gML = g & 0x3;
                            bML = b & 0x3;
                        } else if (x % 2 == 1 && y % 2 == 0) {
                            rLM = r & 0x3;
                            gLM = g & 0x3;
                            bLM = b & 0x3;
                        } else {
                            rLL = r & 0x3;
                            gLL = g & 0x3;
                            bLL = b & 0x3;
                        }
                    }
                }
                r = (rMM << 6) | (rML << 4) + (rLM << 2) + rLL;
                g = (gMM << 6) | (gML << 4) + (gLM << 2) + gLL;
                b = (bMM << 6) | (bML << 4) + (bLM << 2) + bLL;
                if (i / 2 >= dimensions.width || j / 2 >= dimensions.height) {
                    break;
                }
                target.setRGB(i / 2, j / 2, r << 16 | g << 8 | b);
            }
        }
        ImageIO.write(target, "png", new File(targetDirectory + (targetDirectory.endsWith("/") ? "" : "/") + decodeName(source)));
    }

    private static BufferedImage enlargeKeyImageIfNeeded(final BufferedImage image, final int multiplier) {
        final BufferedImage enlarged = new BufferedImage(image.getWidth() * multiplier, image.getHeight() * multiplier, IMAGE_TYPE);
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                for (int x = i * multiplier; x < i * multiplier + multiplier; x++) {
                    for (int y = j * multiplier; y < j * multiplier + multiplier; y++) {
                        enlarged.setRGB(x, y, image.getRGB(i, j));
                    }
                }
            }
        }
        return enlarged;
    }

    private static void encodeImageResolution(final int width, final int height, final BufferedImage target) {
        populateByteData(target, 0, 0, width >> 8);
        populateByteData(target, 1, 0, width & 0xff);
        populateByteData(target, 2, 0, height >> 8);
        populateByteData(target, 3, 0, height & 0xff);
    }

    private static void encodeName(final String name, final BufferedImage target) {
        final int width = target.getWidth();
        int x = 0, y = 1;
        populateByteData(target, x++, y, name.length());
        for (final char ch : name.toCharArray()) {
            if (x >= width) {
                x = 0;
                y++;
            }
            populateByteData(target, x++, y, ch);
        }
    }

    private static void populateByteData(final BufferedImage image, final int x, final int y, int data) {
        final int keyRGB = image.getRGB(x, y) & 0x00ffffff;
        final int rM = (keyRGB >> 16) & 0xf0;
        final int gM = (keyRGB >> 8) & 0xf0;
        final int b = keyRGB & 0xff;

        //ensure data is only one byte
        data = data & 0xff;
        final int dataM = (data & 0x000000f0) >> 4;
        final int dataL = data & 0x0000000f;

        final int r = rM | dataM;
        final int g = gM | dataL;
        image.setRGB(x, y, r << 16 | g << 8 | b);
    }

    private static Tuple decodeImageResolution(final BufferedImage source) {
        final int widthM = extractByteData(source, 0, 0);
        final int widthL = extractByteData(source, 1, 0);
        final int heightM = extractByteData(source, 2, 0);
        final int heightL = extractByteData(source, 3, 0);
        return new Tuple((widthM << 8) | widthL, (heightM << 8) | heightL);
    }

    private static String decodeName(final BufferedImage source) {
        int x = 0, y = 1;
        final int len = extractByteData(source, x++, y);
        final int width = source.getWidth();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (x >= width) {
                x = 0;
                y++;
            }
            builder.append((char) extractByteData(source, x++, y));
        }
        return builder.toString();
    }

    private static int extractByteData(final BufferedImage source, final int x, final int y) {
        final int keyRGB = source.getRGB(x, y) & 0x00ffffff;
        final int rL = (keyRGB >> 16) & 0x0f;
        final int gL = (keyRGB >> 8) & 0x0f;
        return (rL << 4) | gL;
    }

    private static class Tuple {
        private int width;
        private int height;

        private Tuple(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
