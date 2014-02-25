package com.codeminders.hidapi;


import java.io.*;


public class ClassPathLibraryLoader2 {
   public enum OSType {
      Windows, MacOS, Linux, Other;
   }

   public static OSType getOSType() {
      String OS = System.getProperty("os.name", "generic").toLowerCase();
      if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
         return OSType.MacOS;
      } else if (OS.indexOf("win") >= 0) {
         return OSType.Windows;
      } else if (OS.indexOf("nux") >= 0) {
         return OSType.Linux;
      } else {
         return OSType.Other;
      }
   }

   public static String getLibExtensionForOS(OSType os) {
      switch (os) {
         case Windows:
            return "dll";
         case MacOS:
            return "jnilib";
         case Linux:
            return "so";
      }
      return "unknown";
   }

   /**
    * Java's "os.arch" System Property is the Bitness of the JRE, NOT the OS
    */
   public static String getWindowsCPUType() {
      //System.out.println("getWindowsCPUType");
      String arch = System.getenv("PROCESSOR_ARCHITEW6432");
      if (arch != null && !arch.isEmpty()) {
         return arch;
      }
      return System.getenv("PROCESSOR_ARCHITECTURE");
   }

   public static String getLinuxCPUType() {
      //System.out.println("getLinuxCPUType");
      try {
         Process p = Runtime.getRuntime().exec("uname -p");
         p.waitFor();
         BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
         String line;
         try {
            while ((line = in.readLine()) != null) {
               return line;
            }
         } finally {
            in.close();
         }
      } catch (IOException e1) {
         e1.printStackTrace();
      } catch (InterruptedException e2) {
         e2.printStackTrace();
      }
      return "";
   }

   public static String getCPUBitArch() {
      OSType os = getOSType();
      String arch = "32";
      String type = "";
      if (os == OSType.Windows) {
         type = getWindowsCPUType();
      } else if (os == OSType.Linux || os == OSType.MacOS) {
         type = getLinuxCPUType();
      }
      if (type == null) {
         return arch;
      }
      if (type.contains("64")) {
         arch = "64";
      } else if (type.contains("arm")) {//hf or sf ? i dunno
         arch = "arm32";//since v8 there is 64 bit but how to detect?
      }

      return arch;
   }

   public static boolean loadNativeHIDLibrary() throws UnsatisfiedLinkError, Exception {
      OSType os = getOSType();
      String arch = getCPUBitArch();
      String path = "/native/" + os.toString().toLowerCase() + "/libhidapi-jni-" + arch + "." + getLibExtensionForOS(os);
      System.out.println("Trying to load: " + path);

      // have to use a stream
      InputStream in = ClassPathLibraryLoader.class.getResourceAsStream(path);
      if (in != null) {
         try {
            // always write to different location
            String tempName = path.substring(path.lastIndexOf('/') + 1);
            File fileOut = File.createTempFile(tempName.substring(0, tempName.lastIndexOf('.')), tempName.substring(tempName.lastIndexOf('.'), tempName.length()));
            fileOut.deleteOnExit();

            OutputStream out = new FileOutputStream(fileOut);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
               out.write(buf, 0, len);
            }

            out.close();
            Runtime.getRuntime().load(fileOut.toString());
         } finally {
            in.close();
         }
      }

      return true;
   }
}
