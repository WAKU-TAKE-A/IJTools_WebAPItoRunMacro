import ij.*;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import java.awt.AWTEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import net.freeutils.httpserver.HTTPServer.VirtualHost;
import net.freeutils.httpserver.HTTPServer;

/*
 * The MIT License
 *
 * Copyright 2021 Takehito Nishida.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Ver. 0.9.0.1
 * Web API to run macro of ImageJ.
 */
public class WK_WebRunMacro implements ij.plugin.filter.ExtendedPlugInFilter {    
    static HTTPServer server = null;    
    static int port = 1997;
    static boolean no_dialog = false;
    static String status = "start";    
    
    @Override
    public int showDialog(ImagePlus ip, String command, PlugInFilterRunner pifr) {        
        GenericDialog gd = new GenericDialog(command.trim() + " ...");        
        gd.addNumericField("port", port, 0);
        gd.addCheckbox("no_daialog", no_dialog);
        gd.showDialog();

        if (gd.wasCanceled())
        {
            return DONE;
        }
        else
        {
            port = (int)gd.getNextNumber();
            no_dialog = gd.getNextBoolean();
            return IJ.setupDialog(ip, NO_IMAGE_REQUIRED);
        }
    }

    @Override
    public void setNPasses(int i) {        
        // do nothing
    }

    @Override
    public int setup(String string, ImagePlus ip) {
        return NO_IMAGE_REQUIRED;
    }

    @Override
    public void run(ImageProcessor ip) {        
        try {                       
            if (server != null) {
                server.stop();
                server = null;
            }
                        
            server = new HTTPServer(port);
            VirtualHost host = server.getVirtualHost(null);
            
            // コンテキスト追加
            host.addContext("/", (HTTPServer.Request rqst, HTTPServer.Response rspns) -> {
                rspns.getHeaders().add("Content-Type", "text/plain");
                rspns.send(200, "Run WK_HttpRunMacro\n\n/post_image?name=abc :\n\t* POST method.\n\t* Send image to ImageJ.\n\t* Send in byte array.\n/run_macro?macro=hoge&par1=1&par2=2&par3 :\n\t* GET method.\n\t* Run 'hoge' macro.\n\t* Response is JSON.\n\t* JSON consists of Base64 image and log.");
                return 0;
            });
            
            host.addContext("/post_image", (HTTPServer.Request rqst, HTTPServer.Response rspns) -> {
                Map<String, String> params = rqst.getParams();
                String name = params.get("name");
                
                if (name == null) {
                    rspns.send(400, "no name.");
                } else {
                    InputStream inp = rqst.getBody();
                    BufferedImage bImage = ImageIO.read(inp);
                    ImagePlus img = new ImagePlus(name, bImage);
                    img.show();
                    
                    rspns.getHeaders().add("Content-Type", "text/plain");
                    rspns.send(200, "success.");
                }                
              
                return 0;
            }, "POST");
                                  
            host.addContext("/run_macro", (HTTPServer.Request rqst, HTTPServer.Response rspns) -> {
                Map<String, String> params = rqst.getParams();
                String macro_name = params.get("macro");
                String macro_option = "";
                
                if (macro_name == null) {
                    rspns.send(400, "no macro.");
                } else {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        if (!entry.getKey().equals("macro")) {
                            if (entry.getValue() == "") {
                                macro_option = macro_option + " " + entry.getKey();
                            } else {
                                macro_option = macro_option + " " + entry.getKey() + "=" + entry.getValue(); 
                            }
                        }
                    }
                    
                    if (macro_option.equals("")) {
                        IJ.runMacro("run(\"" + macro_name +  "\");");
                    } else {
                        IJ.runMacro("run(\"" + macro_name +  "\", \"" + macro_option + "\");");
                    }
                                        
                    int count = WindowManager.getImageCount();
                    String log = IJ.getLog();
                    String json_string = "";                    
                    
                    if (log != null) {
                        json_string = json_string + "\"log\":\"" + log.replaceFirst("\n$" ,"") + "\"";
                    }
                    
                    if (count != 0) {
                        ImagePlus img = IJ.getImage();
                        String img_base64 = ImagePlus2Base64(img, "jpg");
                        json_string = json_string.equals("") ?  "\"image\":\"" + img_base64 + "\"" : json_string + ", \"image\":\"" + img_base64 + "\"";
                    }
                    
                    json_string = "{" + json_string + "}";
                    
                    rspns.getHeaders().add("Content-Type", "application/json");
                    rspns.send(200, json_string);
                }
                
                return 0;
            });
          
            // ダイアログ表示
            SubClass_ThreadDialog thrddg = new SubClass_ThreadDialog();        
            thrddg.start();
            
            // サーバー起動            
            server.start();
        } catch (IOException ex) {
            server.stop();
            Logger.getLogger(WK_WebRunMacro.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String ImagePlus2Base64(ImagePlus src, String type) {
        BufferedImage buf = src.getBufferedImage();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        buf.flush();
        
        try {
            ImageIO.write(buf, type, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            // do nothing
        }

        byte[] bImage = baos.toByteArray();
        return Base64.getEncoder().encodeToString(bImage);
    }
    
    public byte[] InputStream2Bytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }
    
    public class SubClass_ThreadDialog extends Thread {            
        @Override
        public void run() {
            NonBlockingGenericDialog nbgd = new NonBlockingGenericDialog("HttpRunMacro");
            nbgd.addChoice("status", new String[] { "start", "stop"}, "start");
            nbgd.hideCancelButton();

            nbgd.addDialogListener((GenericDialog gd, AWTEvent awte) -> {
                status = gd.getNextChoice();
                
                if (status.equals("start")) {
                    try {
                        server.start();
                    } catch (IOException ex) {
                        Logger.getLogger(WK_WebRunMacro.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    server.stop();
                }
                
                return true;
            });
            
            nbgd.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    server.stop();
                }
            });

            if (!no_dialog) {
                nbgd.showDialog();
            }            
        }   
    }
    
}
