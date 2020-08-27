// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that sends user data to datastore */
@WebServlet("/user-form")
public class UserDataServlet extends HttpServlet {
  
  private DatastoreService datastore;
  private BlobstoreService blobstoreService;

  public void init(){
    datastore = DatastoreServiceFactory.getDatastoreService();
    blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    String userEmail = userService.getCurrentUser().getEmail();
    long timestamp = System.currentTimeMillis();

    // Fetch user data from filled form on the client side
    String name = request.getParameter("name");
    String department = request.getParameter("department");
    String bio = request.getParameter("bio");
    String profilePictureUrl = getUploadedFileUrl(request, "user-img");

    // Create entity based on user data
    Entity userEntity = new Entity("User");
    userEntity.setProperty("userEmail", userEmail);
    userEntity.setProperty("name", name);
    userEntity.setProperty("department", department);
    userEntity.setProperty("bio", bio);
    userEntity.setProperty("profilePictureUrl", profilePictureUrl);
    userEntity.setProperty("timestamp", timestamp);
    
    // Send user data to datastore
    datastore.put(userEntity);
    response.sendRedirect("/recommendation-map.html");
  }

  // Get URL to the uploaded file
  private String getUploadedFileUrl(HttpServletRequest request, String inputElementName){
    List<BlobKey> blobKeys = blobstoreService.getUploads(request).get(inputElementName);

    // User submitted form without selecting a file, so we can't get a URL. (devserver)
    if(blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // User submitted form without selecting a file, so we can't get a URL. (live server)
    BlobKey blobKey = blobKeys.get(0);
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    // Use ImagesService to get a URL that points to the uploaded file.
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);
    return ImagesServiceFactory.getImagesService().getServingUrl(options);
  }
}
