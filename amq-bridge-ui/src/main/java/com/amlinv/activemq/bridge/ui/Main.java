package com.amlinv.activemq.bridge.ui;

import java.io.File;
import java.net.URL;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

        
      public static void main(String[] args) throws Exception
      {
  
          //The port that we should run on can be set into an environment variable
          //Look for that variable and default to 8080 if it isn't there.
          String port = System.getenv("PORT");
          if(port == null || port.isEmpty()) {
              port = "8081";
          }
          
          String root = System.getenv("JETTY_ROOT");
          if(root == null || root.isEmpty()) {
              root = "src/main/resources";
          }
          
    	  Server server = new Server(Integer.valueOf(port));
  
          ResourceHandler resource_handler = new ResourceHandler();
          resource_handler.setDirectoriesListed(true);
          resource_handler.setWelcomeFiles(new String[]{ "index.html" });
  
          resource_handler.setResourceBase(root);
          LOG.info("serving " + resource_handler.getBaseResource());
          
          HandlerList handlers = new HandlerList();
          handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
          server.setHandler(handlers);
  
          server.start();
          server.join();
      }
      

}
