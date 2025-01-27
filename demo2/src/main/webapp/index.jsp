<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Base64" %>
<%@ page import="util.ImageUtils" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Image Processor</title>
  <style>
    #image-preview {
      max-width: 300px;
      max-height: 300px;
      margin-top: 10px;
    }
  </style>
</head>
<body>

<h1>Image Processor</h1>

<form action="imageProcessor" method="post" enctype="multipart/form-data">
  <label for="image">Choose an image:</label>
  <input type="file" name="image" accept="image/*" onchange="previewImage(this)">
  <br>
  <img id="image-preview" src="#" alt="Image Preview">
  <br>
  <input type="submit" value="Upload and Process">
</form>

<%
  // Check if processedImage attribute is present in the request
  byte[] processedImage = (byte[]) request.getAttribute("processedImage");
  if (processedImage != null) {
    String base64EncodedImage = Base64.getEncoder().encodeToString(processedImage);
%>
<h2>Processed Image</h2>
<img src='data:image/jpeg;base64,<%= base64EncodedImage %>' id="processed-image" alt="Processed Image"/>
<%
  }
%>

<script>
  function previewImage(input) {
    var preview = document.getElementById('image-preview');

    var reader = new FileReader();
    reader.onload = function(){
      preview.src = reader.result;
    };
    reader.readAsDataURL(input.files[0]);
  }
</script>

</body>
</html>