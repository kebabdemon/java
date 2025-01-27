package util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageUtils {

    public static byte[] processImage(byte[] originalImageData) {
        try {

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalImageData);
            BufferedImage originalImage = ImageIO.read(byteArrayInputStream);


            BufferedImage processedImage = convertToGrayscale(originalImage);


            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "jpeg", byteArrayOutputStream);

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BufferedImage convertToGrayscale(BufferedImage originalImage) {

        BufferedImage processedImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        processedImage.getGraphics().drawImage(originalImage, 0, 0, null);

        return processedImage;
    }
}
