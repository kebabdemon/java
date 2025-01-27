import util.ImageUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.IOException;
import java.util.List;

@WebServlet("/imageProcessor")
public class ImageServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (ServletFileUpload.isMultipartContent(request)) {
            try {

                List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);

                for (FileItem item : items) {
                    if (!item.isFormField() && "image".equals(item.getFieldName())) {

                        byte[] originalImageData = item.get();
                        byte[] processedImageData = ImageUtils.processImage(originalImageData);
                        request.setAttribute("processedImage", processedImageData);
                        break;
                    }
                }
            } catch (Exception e) {

                e.printStackTrace();
            }
        }


        RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
        dispatcher.forward(request, response);
    }
}