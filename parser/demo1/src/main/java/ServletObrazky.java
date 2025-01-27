


import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import com.jhlabs.image.GammaFilter;
import com.jhlabs.image.GaussianFilter;

/**
 *
 * @author UserXP
 */
@WebServlet("/ServletObrazky")
public class ServletObrazky extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private BufferedImage transformed;
    private InputStream original_input_stream;
    private String original_content_type;
    private BufferedImage original;
    private void printForm(PrintWriter out, int selRadius) {
        int radiuses[] = {1, 5, 10, 20, 50};
        out.println("<form  method='post' enctype='multipart/form-data'>\n"
                + "<input name='image' type='file'/>"
        		+ "<select name='type'><option value='0'>Gauss</option><option value='1'>Gamma</option></select>\n"
                + "Radius:"
                + "<select name='radius'>\n");
        for (int r : radiuses) {
            out.print("<option value='" + r + "'");
            if (r == selRadius) {
                out.print("selected='selected'");
            }
            out.println(">" + r + "</option>");
        }
        out.println("</select>\n"
                + "<input type='submit' value='Odeslat'>\n"
                + "<form>");

    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        int selRadius = 1;
        int type = 0;

        if (ServletFileUpload.isMultipartContent(request)) {
            try {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload fileupload = new ServletFileUpload(factory);
				List<FileItem> items = fileupload.parseRequest(request);
                // Pruchod fieldem
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        String fieldName = item.getFieldName();
                        String fieldValue = item.getString();
                        if (fieldName.equals("radius")) {
                            out.println("Radius=" + fieldValue);
                            selRadius = Integer.parseInt(fieldValue);                            
                        }
                        if(fieldName.equals("type"))
                        {
                        	type = Integer.parseInt(fieldValue);
                        }
                    } else {    //item je typu fileupload
                        String filename=item.getName();
                        long size=item.getSize();
                        if (!filename.isEmpty() && size>0) {
                            BufferedInputStream new_input_stream = new BufferedInputStream(item.getInputStream());
                            new_input_stream.mark((int)item.getSize()+1);
                            BufferedImage new_original = ImageIO.read(new_input_stream);
                            if (new_original != null) {
                                if (original_input_stream!=null)
                                    original_input_stream.close();
                                original_input_stream=new_input_stream;                                
                                original_content_type = item.getContentType();
                                original=new_original;
                            }

                        }
//                        out.println("size="+item.getSize()+", content type="+item.getContentType());
                    }
                }
                	if (original != null) {
                	
                	if(type == 0)
                	{
                        GaussianFilter gf = new GaussianFilter(selRadius);
                        transformed = gf.filter(original, null);
                	}
                	else
                	{
                        GammaFilter gam = new GammaFilter(selRadius);
                        transformed = gam.filter(original, null);
                	}
                }

            } catch (FileUploadException ex) {
                System.out.println(ex);
            }
        }


        try {
            /*
             * TODO output your page here. You may use following sample code.
             */
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet FourierTransform</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Ahoj sv�te, Servlet FourierTransform at " + request.getContextPath() + "</h1>");
            printForm(out, selRadius);
            String servlet = request.getRequestURI();
            if (original != null) {
                out.println("<p><img src='" + servlet + "?getimage=original'</br>");
            }
            if (transformed != null) {
                out.println("<p><img src='" + servlet + "?getimage=transformed'</br>");
            }
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String param_getimage = request.getParameter("getimage");
        if (original_input_stream!=null && param_getimage != null) {
            OutputStream out = response.getOutputStream();
            if (param_getimage.equals("original")) {
                original_input_stream.reset();
                response.setContentType(original_content_type);
                IOUtils.copy(original_input_stream,out); 
            } else if (param_getimage.equals("transformed")) {
                response.setContentType("image/jpeg");
                ImageIO.write(transformed, "jpeg", out);
            }
            out.close();
        } else {
            processRequest(request, response);
//            response.setContentType("text/html;charset=UTF-8");
            //          PrintWriter out = response.getWriter();
        }

    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}