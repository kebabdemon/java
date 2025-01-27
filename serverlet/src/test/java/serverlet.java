import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/imageProcessor")
@MultipartConfig
public class ImageProcessorServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Part filePart = request.getPart("image");
        InputStream fileContent = filePart.getInputStream();
        BufferedImage originalImage = ImageIO.read(fileContent);

        // Apply selected filters
        BufferedImage convolvedImage = ImageFilter.convolveFilter(originalImage);
        BufferedImage boxBlurredImage = ImageFilter.boxBlurFilter(originalImage, 5, 5, 3);
        BufferedImage gaussianImage = ImageFilter.gaussianFilter(originalImage, 5);

        // Display the images on the HTML page
        response.setContentType("text/html");
        response.getWriter().println("<html><body>");
        response.getWriter().println("<h2>Original Image</h2>");
        response.getWriter().println("<img src='data:image/png;base64," + ImageUtils.encodeImage(originalImage) + "'/><br>");

        response.getWriter().println("<h2>Convolved Image</h2>");
        response.getWriter().println("<img src='data:image/png;base64," + ImageUtils.encodeImage(convolvedImage) + "'/><br>");

        response.getWriter().println("<h2>Box Blurred Image</h2>");
        response.getWriter().println("<img src='data:image/png;base64," + ImageUtils.encodeImage(boxBlurredImage) + "'/><br>");

        response.getWriter().println("<h2>Gaussian Image</h2>");
        response.getWriter().println("<img src='data:image/png;base64," + ImageUtils.encodeImage(gaussianImage) + "'/><br>");

        response.getWriter().println("</body></html>");
    }
}

class ImageFilter {

    public static BufferedImage convolveFilter(BufferedImage originalImage) {
        float[] matrix = {
                0.1f, 0.1f, 0.1f,
                0.1f, 0.2f, 0.1f,
                0.1f, 0.1f, 0.1f
        };
        Kernel kernel = new Kernel(3, 3, matrix);
        ConvolveOp op = new ConvolveOp(kernel);
        return op.filter(originalImage, null);
    }

    public static BufferedImage boxBlurFilter(BufferedImage originalImage, int hRadius, int vRadius, int iterations) {
        Kernel kernel = new Kernel(3, 3, new float[]{1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f});
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        BufferedImage result = originalImage;
        for (int i = 0; i < iterations; i++) {
            result = op.filter(result, null);
        }
        return result;
    }

    public static BufferedImage gaussianFilter(BufferedImage originalImage, int radius) {
        float[] matrix = createGaussianMatrix(radius);
        Kernel kernel = new Kernel(radius * 2 + 1, radius * 2 + 1, matrix);
        ConvolveOp op = new ConvolveOp(kernel);
        return op.filter(originalImage, null);
    }

    private static float[] createGaussianMatrix(int radius) {
        int size = radius * 2 + 1;
        float[] matrix = new float[size * size];
        float sigma = radius / 3.0f;
        float sigma22 = 2 * sigma * sigma;
        float sigmaPi2 = (float) (2 * Math.PI * sigma * sigma);
        float sqrtSigmaPi2 = (float) Math.sqrt(sigmaPi2);
        int index = 0;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float distance = x * x + y * y;
                matrix[index] = (float) Math.exp(-distance / sigma22) / sqrtSigmaPi2;
                index++;
            }
        }
        return matrix;
    }
}

class ImageUtils {
    public static String encodeImage(BufferedImage image) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] byteArray = baos.toByteArray();
            return Base64.getEncoder().encodeToString(byteArray);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
